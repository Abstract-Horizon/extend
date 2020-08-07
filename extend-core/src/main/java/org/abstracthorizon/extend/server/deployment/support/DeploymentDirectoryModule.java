/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.extend.server.deployment.support;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleUtils;
import org.abstracthorizon.extend.server.support.Dump;
import org.abstracthorizon.extend.server.support.KernelScheduler;
import org.abstracthorizon.extend.server.support.SimpleScheduler;

/**
 * This module scans given URL for changes and then deploys/undeploys/redeploys modules added/removed/changed to it.
 * It uses {@link KernelScheduler} for intervals for scanning.
 *
 * @author Daniel Sendula
 */
public class DeploymentDirectoryModule implements Module, Runnable {

    /** Module name */
    protected ModuleId moduleId;

    /** Path */
    protected File path;

    /** Original location url */
    protected URL location;

    /** Module state */
    protected int state = Module.UNDEFINED;

    /** List of modules that depend on this */
    protected LinkedHashSet<Module> dependOnThis = new LinkedHashSet<Module>();

    /** Scheduler to check changes in this directory */
    protected SimpleScheduler scheduler;

    /** Map of files' last changed time from supplied directory */
    protected HashMap<File, Long> files = new HashMap<File, Long>();

    /** Deployment manager everything is executed from */
    protected DeploymentManager manager;

    /** Empty constructor */
    public DeploymentDirectoryModule() {
    }

    /**
     * Returns <code>null</code>.
     * @return <code>null</code>
     */
    public ClassLoader getClassLoader() {
        return null;
    }

    /**
     * Returns set of modules that depend on this.
     * @return set of modules that depend on this
     */
    public Set<Module> getDependOnThis() {
        return dependOnThis;
    }

    /**
     * Returns an empty set.
     * @return en empty set
     */
    public Set<Module> getDependsOn() {
        return ModuleUtils.EMPTY_SET;
    }

    /**
     * Returns original location url.
     * @return original location url
     */
    public URL getOriginalLocation() {
        return location;
    }

    /**
     * Returns working location url.
     * @return working location url
     */
    public URL getWorkingLocation() {
        return location;
    }

    /**
     * Sets location.
     * @param location location
     * @throws RuntimeException if url is not of file protocol and file part is not ending with &quot;/&quot;
     */
    public void setLocation(URL location) {
        try {
            String file = URLDecoder.decode(location.getFile(), "UTF-8");
            if (!"file".equals(location.getProtocol())) {
                throw new RuntimeException("Location must be of file protocol");
            }
            if (!file.endsWith("/")) {
                throw new RuntimeException("Location has to be path ending with '/'");
            }
            this.location = location;
            this.path = new File(file);
        } catch (UnsupportedEncodingException ignore) {
        }
    }

    /**
     * Sets location as a file
     * @param file directory
     * @throws RuntimeException if file is not a directory
     */
    public void setLocation(File file) {
        if (file.exists() && !file.isDirectory()) {
            throw new RuntimeException("File has to be directory");
        }
        this.path = file;
        try {
            this.location = new URL("file", null, 0, file.getAbsolutePath());
        } catch (MalformedURLException ignore) {
        }
    }

    /**
     * Returns module's id
     * @return module's id
     */
    public ModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Sets module's id
     * @param moduelId module's id
     */
    public void setModuleId(ModuleId moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Does nothing
     */
    public void create() {
        setState(Module.CREATED);
    }

    /**
     * Start scheduler over this directory
     */
    public void start() {
        scheduler.schedule(this);
        setState(Module.STARTED);
    }

    /**
     * Stops scheduler
     */
    public void stop() {
        scheduler.remove(this);
        setState(Module.CREATED);
    }

    /**
     * Trys to undeploy files that were watched over
     */
    public void destroy() {
        for (File file : files.keySet()) {
            undeploy(file);
        }
        setState(Module.DEFINED);
    }

    /**
     * Calls {@link #rescan()}.
     */
    public void run() {
        try {
            rescan();
        } catch (Throwable e) {
            e.printStackTrace();
            Extend.info.error("Detected error - making extend-core.out!");
            Dump.outputCore(manager);
            System.exit(-1);
        }
    }

    /**
     * Scans given directory for changes. If file is new it is going to be deployed
     * through main deployer {@link DeploymentManager}. If file already existed and
     * last changed time is changed it is going to be redeployed.
     *
     */
    @SuppressWarnings("unchecked")
    protected synchronized void rescan() {
        Map<File, Long> o = (Map<File, Long>)files.clone();
        File[] fs = path.listFiles();
        if (fs != null) {
            for (File file : fs) {
                Long lastChange = o.get(file);
                if (lastChange == null) {
                    files.put(file, new Long(file.lastModified()));
                    Extend.info.debug("Deploying file " + file);
                    deploy(file);
                } else if ((lastChange.longValue() != file.lastModified())) {
                    Extend.info.debug("Redeploying file " + file);
                    if (Extend.debug.isDebugEnabled()) {
                        Extend.info.debug("Redeploying file " + file + "; lastChange=" + lastChange.longValue() + ", fileModified=" + file.lastModified());
                    }
                    o.remove(file);
                    redeploy(file);
                    files.put(file, new Long(file.lastModified()));
                } else {
                    o.remove(file);
                }
            }
        }
        for (File file : o.keySet()) {
            files.remove(file);
            Extend.info.debug("Undeploying file " + file);
            undeploy(file);
        }
    }

    /**
     * Deploys file loading module through main deployer {@link DeploymentManager}.
     * @param file file to be deployed
     */
    public void deploy(File file) {
        URI uri = file.toURI();
        Module module = manager.load(uri);
        if (module == null) {
            ModuleId moduleId = ModuleId.createModuleIdFromFileName(file);
            module = new ProvisionalModule(uri, moduleId); // TODO this should not be in here but deployment manager ONLY!
        }
        ModuleId moduleId = module.getModuleId();

        Module other = manager.getDeployedModules().get(moduleId);
        if ((other != null) && (other != module) && !(other instanceof ProvisionalModule)) {
            Extend.info.warn("There is already module with moduleId " + moduleId + " deployed. Ignoring this file: " + file.getAbsolutePath());
            //throw new RuntimeException("Overwriting existing bean!");
        } else {
            try {
                manager.deploy(moduleId, module);
            } catch (Exception e) {
                Extend.info.warn("Caught exception trying to deploy module " + moduleId + ".", e);
            }
        }
    }

    /**
     * Re-deploys file finding module through main deployer {@link DeploymentManager}.
     * @param file file to be redeployed
     */
    public void redeploy(File file) {
        ModuleId moduleId = ModuleId.createModuleIdFromFileName(file);
        Module module = manager.getDeployedModules().get(moduleId);
        if (module != null) {
            manager.redeploy(module);
        } else {
            try {
                manager.deploy(moduleId, module);
            } catch (Exception e) {
                Extend.info.warn("Caught exception trying to deploy module " + moduleId + ".", e);
            }
        }
    }

    /**
     * Undeploys file finding module through main deployer {@link DeploymentManager}.
     * @param file file to be undeployed
     */
    public void undeploy(File file) {
        ModuleId moduleId = ModuleId.createModuleIdFromFileName(file);
        Module module = manager.getDeployedModules().get(moduleId);
        if (module != null) {
            try {
                manager.undeploy(module);
            } catch (Exception e) {
                Extend.info.warn("Caught exception trying to undeploy module " + moduleId + ".", e);
            }
        }
    }

    /**
     * Sets module's state
     * @param state module's state
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * Returns module's state
     * @return module's state
     */
    public int getState() {
        return state;
    }

    /**
     * Returns module's state as a string
     * @return module's state as a string
     */
    public String getStateAsString() {
        return ModuleUtils.stateAsString(state);
    }

    /**
     * Sets scheduler ({@link SimpleScheduler}).
     * @param scheduler scheduler
     */
    public void setScheduler(SimpleScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Returns scheduler ({@link SimpleScheduler})
     * @return scheduler
     */
    public SimpleScheduler getScheduler() {
        return scheduler;
    }

    /**
     * This method is used internally for scheduler to be obtained.
     * If scheduler is not assigned then {@link KernelScheduler} is
     * going to be used.
     * @return scheduler or newly created scheduler
     */
    protected SimpleScheduler getSchedulerInternal() {
        if (scheduler == null) {
            scheduler = new KernelScheduler();
        }
        return scheduler;
    }

    /**
     * Returns main deployer ({@link DeploymentManager});
     * @return main deployer ({@link DeploymentManager});
     */
    public DeploymentManager getDeploymentManager() {
        return manager;
    }

    /**
     * Sets main deployer ({@link DeploymentManager});
     * @param manager main deployer ({@link DeploymentManager});
     */
    public void setDeploymentManager(DeploymentManager manager) {
        this.manager = manager;
    }

    /**
     * Compares two modules and returns <code>true</code> if names are the same
     * @param o other object
     * @return <code>true</code> if names are the same
     */
    public boolean equals(Object o) {
        if ((o instanceof Module) && getModuleId().equals(((Module)o).getModuleId())) {
            return true;
        }
        return super.equals(o);
    }

}
