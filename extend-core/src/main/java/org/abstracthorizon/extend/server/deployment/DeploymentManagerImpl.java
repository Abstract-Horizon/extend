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
package org.abstracthorizon.extend.server.deployment;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.support.ProvisionalModule;
import org.abstracthorizon.extend.server.support.EnhancedMap;

/**
 * Implementation of {@link DeploymentManager}.
 *
 * @author Daniel Sendula
 */
public class DeploymentManagerImpl implements DeploymentManager {

    /** List of module loaders */
    protected ModuleLoaders<ModuleLoader> moduleLoaders = new ModuleLoaders<ModuleLoader>();

    /** Map of deployed modules */
    protected DeployedModules<ModuleId, Module> deployedModules = new DeployedModules<ModuleId, Module>();

    /** Empty constructor */
    public DeploymentManagerImpl() {
    }

    /**
     * Checks if module is deployed.
     * @param module module
     * @return <code>true</code> if given module is deployed
     */
    public boolean isDeployed(Module module) {
//        Module m = deployedModules.get(module.getName());
//        return m == module;
        return deployedModules.containsValue(module);
    }

    /**
     * Deploys given module. It calls {@link Module#create()} and {@link Module#start()} methods.
     *
     * @param module module
     * @throws RuntimeException if module with the same name is already deployed (and is not {@link ProvisionalModule}
     */
    public void deploy(ModuleId moduleId, Module module) {
        Module old = deployedModules.get(moduleId);
        if (old != module) {
            long startedTime = 0;
            if (Extend.info.isDebugEnabled()) {
                startedTime = System.currentTimeMillis();
                Extend.info.debug("Deploying module '" + moduleId + "' of class=" + module.getClass());
            } else if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Deploying module '" + moduleId + "'");
            }
            if (old != null) {
                if (!(old instanceof ProvisionalModule)) {
                    String errorMsg = "Module with id '" + moduleId + "' already exists; class=" + old.getClass() + ". Cannot deploy it.";
                    Extend.info.error(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
                deployedModules.remove(old.getModuleId());
            }
            deployedModules.put(moduleId, module);

            // Old module is just ProvisionalModule. Update references
            if (old != null) {
                if (Extend.info.isDebugEnabled()) {
                    Extend.info.debug("Updating references to this module from provisional module; id=" + moduleId + "'");
                }
                for (Module m : old.getDependsOn()) {
                    m.getDependOnThis().remove(old);
                    m.getDependOnThis().add(module);
                }
                for (Module m : old.getDependOnThis()) {
                    m.getDependsOn().remove(old);
                    m.getDependsOn().add(module);
                }

                module.getDependOnThis().addAll(old.getDependOnThis());
            }

            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Creating module '" + moduleId + "'.");
            }
            create(module);
            if ((module.getState() == Module.CREATED) || (module.getState() == Module.WAITING_ON_CREATE)) {
                start(module);
            }
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Deployed module '" + moduleId + "' (" + Long.toString(System.currentTimeMillis() - startedTime) + "ms)");
            }
        }
    }

    /**
     * Undeploys given module. It calls {@link Module#stop()} and
     * {@link Module#destroy()} methods.
     * @param module module
     */
    public void undeploy(Module module) {
        ModuleId moduleId = module.getModuleId();
        if (Extend.info.isInfoEnabled()) {
            Extend.info.info("Undeploying module '" + moduleId + "'.");
        }
        int state = module.getState();
        if ((state == Module.STARTED) || (state == Module.WAITING_ON_CREATE_TO_START)) {
            stop(module);
        }
        destroy(module);

        deployedModules.remove(moduleId);
        if (Extend.info.isInfoEnabled()) {
            Extend.info.info("Module '" + moduleId + "' is now undeployed.");
        }
    }

    /**
     * Re-deploys given module. Effectively it calls {@link #undeploy(Module)} and
     * {@link #deploy(Module)} methods.
     * @param module module
     */
    public void redeploy(Module module) {
        ModuleId moduleId = module.getModuleId();
        if (Extend.info.isInfoEnabled()) {
            Extend.info.info("Redeploying module '" + moduleId + "'.");
        }
        if (!deployedModules.containsValue(module)) {
            throw new RuntimeException("Module is not deployed");
        }

        Set<ModuleId> aliases = findAliases(module);
        Iterator<ModuleId> it = aliases.iterator();
        undeploy(module);
        deploy(it.next(), module);
        while (it.hasNext()) {
            deployedModules.put(it.next(), module);
        }
    }

    /**
     * Returns all aliases module is known under
     * @param module module
     * @return set of aliases
     */
    public Set<ModuleId> findAliases(Module module) {
        Set<ModuleId> result = new HashSet<ModuleId>();
        for (Map.Entry<ModuleId, Module> entry : deployedModules.entrySet()) {
            if (entry.getValue().equals(module)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Calls {@link Module#create} method.
     * @param module module whose create method is to be called
     */
    public void create(Module module) {
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Creating module '" + module.getModuleId() + "'.");
        }
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        ClassLoader newcl = module.getClassLoader();
        if (newcl != null) {
            Thread.currentThread().setContextClassLoader(newcl);
        }
        try {
            module.create();
        } catch (RuntimeException e) {
            Extend.info.error("Caught exception while creating module " + module, e);
            throw e;
        } catch (Exception e) {
            Extend.info.error("Caught exception while creating module " + module, e);
            throw new RuntimeException(e);
        } finally {
            if (newcl != null) {
                Thread.currentThread().setContextClassLoader(oldcl);
            }
        }
        if (module.getState() == Module.CREATED) {
            if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Created module '" + module.getModuleId() + "'.");
            }
            if (!module.getDependOnThis().isEmpty()) {
                if (Extend.info.isDebugEnabled()) {
                    Extend.info.debug("Creating dependent modules of module '" + module.getModuleId() + "'.");
                }
                for (Module m : module.getDependOnThis()) {
                    if (m.getState() == Module.WAITING_ON_CREATE) {
                        create(m);
                    }
                    if (m.getState() == Module.WAITING_ON_CREATE_TO_START) {
                        create(m);
                        if (m.getState() == Module.CREATED) {
                            m.setState(Module.WAITING_ON_START);
                        }
                    }
                }
            }
        } else if ((module.getState() == Module.WAITING_ON_CREATE) || (module.getState() == Module.WAITING_ON_CREATE_TO_START)) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Creating module '" + module.getModuleId() + "' postponed until dependencies available: " + getUnsatisfiedDependenciesAsAList(module));
            }
        } else {
            if (Extend.info.isWarnEnabled()) {
                Extend.info.warn("Creating module '" + module.getModuleId() + "' failed.");
            }
        }
    }

    /**
     * Calls {@link Module#start} method.
     * @param module module whose start method is to be called
     */
    public void start(Module module) {
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Starting module '" + module.getModuleId() + "'.");
        }
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        ClassLoader newcl = module.getClassLoader();
        if (newcl != null) {
            Thread.currentThread().setContextClassLoader(newcl);
        }
        try {
            module.start();
        } catch (RuntimeException e) {
            Extend.info.error("Caught exception while starting module " + module, e);
            throw e;
        } catch (Exception e) {
            Extend.info.error("Caught exception while starting module " + module, e);
            throw new RuntimeException(e);
        } finally {
            if (newcl != null) {
                Thread.currentThread().setContextClassLoader(oldcl);
            }
        }
        if (module.getState() == Module.STARTED) {
            if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Started module '" + module.getModuleId() + "'.");
            }
            if (!module.getDependOnThis().isEmpty()) {
                if (Extend.info.isDebugEnabled()) {
                    Extend.info.debug("Starting dependent modules of module '" + module.getModuleId() + "'.");
                }
                for (Module m : module.getDependOnThis()) {
                    if (m.getState() == Module.WAITING_ON_CREATE) {
                        create(m);
                    }
                    if (m.getState() == Module.WAITING_ON_CREATE_TO_START) {
                        create(m);
                        if (m.getState() == Module.CREATED) {
                            m.setState(Module.WAITING_ON_START);
                        }
                    }
                    if (m.getState() == Module.WAITING_ON_START) {
                        try {
                            start(m);
                        } catch (RuntimeException e) {
                            Extend.info.error("Caught exception while starting module " + module + " - dependent module " + m.getModuleId() + " start failed", e);
                        }
                    }
                }
            }
        } else if ((module.getState() == Module.WAITING_ON_START) || (module.getState() == Module.WAITING_ON_CREATE_TO_START)) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Starting module '" + module.getModuleId() + "' postponed until dependencies available: " + getUnsatisfiedDependenciesAsAList(module));
            }
        } else {
            if (Extend.info.isWarnEnabled()) {
                Extend.info.warn("Starting module '" + module.getModuleId() + "' failed.");
            }
        }
    }

    /**
     * Calls {@link Module#stop} method.
     * @param module module whose stop method is to be called
     */
    public void stop(Module module) {
        if ((module.getState() == Module.STARTED) && !module.getDependOnThis().isEmpty()) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Stopping dependent modules of module '" + module.getModuleId() + "'.");
            }
            for (Module m : module.getDependOnThis()) {
                if (m.getState() == Module.STARTED) {
                    try {
                        stop(m);
                    } catch (RuntimeException e) {
                        Extend.info.debug("Caught exception while tryging to stop '" + module.getModuleId() + "' module's dependant module '" + m.getModuleId() + "'.", e);
                    }
                    if (m.getState() == Module.CREATED) {
                        m.setState(Module.WAITING_ON_START);
                    } else {
                        if (Extend.info.isWarnEnabled()) {
                            Extend.info.warn("Stopping module '" + module.getModuleId() + "' failed because of stopping of dependent module " + m.getModuleId() + " failed.");
                        }
                    }
                }
                if (m.getState() == Module.STARTED) {
                    throw new DeploymentException("Cannot stop module " + module.getModuleId() + " since dependent module " + m.getModuleId() + " in in illegal state " + ModuleUtils.stateAsString(m.getState()) + ".");
                }
            }
        }
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Stopping module '" + module.getModuleId() + "'.");
        }
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        ClassLoader newcl = module.getClassLoader();
        if (newcl != null) {
            Thread.currentThread().setContextClassLoader(newcl);
        }
        try {
            module.stop();
        } catch (RuntimeException e) {
            Extend.info.error("Caught exception while stopping module " + module, e);
            throw e;
        } catch (Exception e) {
            Extend.info.error("Caught exception while stopping module " + module, e);
            throw new RuntimeException(e);
        } finally {
            if (newcl != null) {
                Thread.currentThread().setContextClassLoader(oldcl);
            }
        }
        if (module.getState() != Module.STARTED) {
            if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Stopped module '" + module.getModuleId() + "'.");
            }
        } else {
            if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Stoping module '" + module.getModuleId() + "' failed.");
            }
        }
    }

    /**
     * Calls {@link Module#destroy} method.
     * @param module module whose destroy method is to be called
     */
    public void destroy(Module module) {
        if ((module.getState() == Module.CREATED) && !module.getDependOnThis().isEmpty()) {
            if (Extend.info.isDebugEnabled()) {
                Extend.info.debug("Destroying dependent modules of module '" + module.getModuleId() + "'.");
            }
            for (Module m : module.getDependOnThis()) {
                if (m.getState() == Module.CREATED) {
                    destroy(m);
                    if (m.getState() == Module.DEFINED) {
                        m.setState(Module.WAITING_ON_CREATE);
                    } else {
                        if (Extend.info.isWarnEnabled()) {
                            Extend.info.warn("Stopping module '" + module.getModuleId() + "' failed because of stopping of dependent module " + m.getModuleId() + " failed.");
                        }
                        return;
                    }
                } else if (m.getState() == Module.WAITING_ON_START) {
                    destroy(m);
                    if (m.getState() == Module.DEFINED) {
                        m.setState(Module.WAITING_ON_CREATE_TO_START);
                    } else {
                        if (Extend.info.isWarnEnabled()) {
                            Extend.info.warn("Stopping module '" + module.getModuleId() + "' failed because of stopping of dependent module " + m.getModuleId() + " failed.");
                        }
                        return;
                    }
                }
                if ((m.getState() != Module.DEFINED) && (m.getState() != Module.WAITING_ON_CREATE_TO_START) && (m.getState() == Module.WAITING_ON_CREATE)) {
                    throw new DeploymentException("Cannot destroy module " + module.getModuleId() + " since dependent module " + m.getModuleId() + " in in illegal state " + ModuleUtils.stateAsString(m.getState()) + ".");
                }
            }
        }
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Destroying module '" + module.getModuleId() + "'.");
        }
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        ClassLoader newcl = module.getClassLoader();
        if (newcl != null) {
            Thread.currentThread().setContextClassLoader(newcl);
        }
        try {
            module.destroy();
        } catch (Exception e) {
            Extend.info.error("Caught exception while destroying module " + module, e);
        } finally {
            if (newcl != null) {
                Thread.currentThread().setContextClassLoader(oldcl);
            }
        }
        if (module.getState() == Module.DEFINED) {
            if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Destroyed module '" + module.getModuleId() + "'.");
            }
        } else {
            if (Extend.info.isInfoEnabled()) {
                Extend.info.info("Destroying module '" + module.getModuleId() + "' failed.");
            }
        }
    }
//
//    /**
//     * Finds module by module's uri. This method can create {@link ProvisionalModule} if module
//     * with given name doesn't exist and createProvisinal parameter is set to <code>true</code>.
//     * {@link ProvisionalModule}'s semantics is of a placeholder for a module that is supposed
//     * to be deployed in near future. It usually comes from dependency decleration between modules.
//     *
//     * @param name name of module
//     * @param createProvisional should provisional module be created for this name
//     * @return module or <code>null</code>
//     */
//    public Module findModule(ModuleId moduleId, boolean createProvisional) {
//        Module module = (Module)deployedModules.get(moduleId);
//        if ((module == null) && createProvisional) {
//            module = new ProvisionalModule(moduleId);
//            deployedModules.put(moduleId, module);
//        }
//        return module;
//    }

    /**
     * Returns map of deployed modules.
     * @return map of deployed modules
     */
    public EnhancedMap<ModuleId, Module> getDeployedModules() {
        return deployedModules;
    }

    /**
     * Returns <code>true</code> if any of registered module loaders knows how to load (create) module from given url.
     * @param url url
     * @return <code>true</code> if module knows how to load (create) module from given url
     */
    public boolean canLoad(URI url) {
        for (ModuleLoader loader : moduleLoaders) {
            if (loader.canLoad(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Translates URI to moduleId
     * @param uri uri
     * @return module id or <code>null</code>
     */
    public ModuleId toModuleId(URI uri) {
        for (ModuleLoader loader : moduleLoaders) {
            if (loader.canLoad(uri)) {
                ModuleId moduleId = loader.toModuleId(uri);
                if (moduleId != null) {
                    return moduleId;
                }
            }
        }
        return null;
    }


    /**
     * Loads module from given URI. If none of registered module loaders can load the module
     * new {@link ProvisionalModule} is created with the name of last part of the URI.
     *
     * will return <code>null</code>.
     * @param uri URI for module to be loaded from
     * @return module from given URI
     */
    public Module load(URI uri) {
        Module module = null;
        for (ModuleLoader loader : moduleLoaders) {
            if (loader.canLoad(uri)) {
                module = loader.load(uri);
                return module;
            }
        }
//        if (module == null) {
//            module = new UnknownLoaderModule(uri);
//        }

        return module;
    }

    /**
     * Loads module from given URI. If none of registered module loaders can load the module
     * new {@link ProvisionalModule} is created with the name of last part of the URI.
     *
     * will return <code>null</code>.
     * @param uri URI for module to be loaded from
     * @param moduleId module id to be used in final module
     * @return module from given URI
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        Module module = null;
        for (ModuleLoader loader : moduleLoaders) {
            if (loader.canLoad(uri)) {
                module = loader.loadAs(uri, moduleId);
                return module;
            }
        }
        return module;
    }


    /**
     * Utility method that loads and deploys module in one go.
     * @param uri module's URI
     */
    public Module loadAndDeploy(URI uri) {
        ModuleId moduleId = toModuleId(uri);
        Module module = null;
        if (moduleId != null) {
            module = getDeployedModules().get(moduleId);
        }
        if (module == null) {
            module = load(uri);
            if (module != null) {
                if (!(module instanceof ProvisionalModule)) {
                    if (!getDeployedModules().containsValue(module)) {
                        deploy(module.getModuleId(), module);
                    } else if (!getDeployedModules().containsKey(module.getModuleId())) {
                        getDeployedModules().put(module.getModuleId(), module);
                    } else {
                        Extend.info.debug("Module " + module.getModuleId() + " at " + uri + " already exists");
                    }
                }
            } else {
                // TODO What here? Throw an exception? Return null?
            }
        }
        return module;
    }
//
//    /**
//     * Utility method that finds module by URI and undeploys it
//     * @param uri module's URI
//     */
//    public void findAndUndeploy(URI uri) {
//        Module module = getDeployedModules().get(uri);
//        if (module != null) {
//            undeploy(module);
//        }
//    }

    /**
     * Utility method
     * @param uri URI
     */
    public void redeploy(ModuleId moduleId) {
        Module module = getDeployedModules().get(moduleId);
        undeploy(module);
        deploy(moduleId, module);
    }

    /**
     * Returns set of module loaders
     * @return module loaders
     */
    public Set<ModuleLoader> getModuleLoaders() {
        return moduleLoaders;
    }

    /**
     * Sets module loaders. Given set is copied into internal representation that is previously cleared down.
     * @param moduleLoaders module loaders
     */
    public void setModuleLoaders(Set<ModuleLoader> moduleLoaders) {
        this.moduleLoaders.clear();
        this.moduleLoaders.addAll(moduleLoaders);
    }

    /**
     * This method goes through all modules with unknown loaders and
     * tries to load them again (using all registered module loaders)
     *
     */
    protected void tryToDeployUnknown() {
        Map<ModuleId, Module> toBeDeployed = new LinkedHashMap<ModuleId, Module>();
        Iterator<Module> it = deployedModules.values().iterator();
        while (it.hasNext()) {
            Module module = it.next();
            if (module instanceof ProvisionalModule) {
                URI uri = ((ProvisionalModule)module).getURI();
                if ((uri != null) && canLoad(uri)) {
                    it.remove();
                    Module m = loadAs(uri, module.getModuleId());
                    toBeDeployed.put(module.getModuleId(), m);
                }
            }
        }
        for (Map.Entry<ModuleId, Module> entry: toBeDeployed.entrySet()) {
            deploy(entry.getKey(), entry.getValue());
        }
    }

    /**
     * This class is extension of {@link LinkedHashSet} that
     * on adding new element calls {@link DeploymentManagerImpl#tryToDeployUnknown()} method.
     *
     */
    protected class ModuleLoaders<E> extends LinkedHashSet<E> {

        /**
         * When new element is added to the list
         * {@link DeploymentManagerImpl#tryToDeployUnknown()} method is called.
         * @param o new module loader
         * @return result of {@link Collection#add(Object)} method
         */
        public boolean add(E o) {
            boolean res = super.add(o);
            if (deployedModules.getUnknownLoaderModulesCount() > 0) {
                tryToDeployUnknown();
            }
            return res;
        }

    }


    protected static String getUnsatisfiedDependenciesAsAList(Module module) {
        StringBuffer res = new StringBuffer();
        boolean first = true;


        if (!module.getDependsOn().isEmpty()) {
            for (Module m: module.getDependsOn()) {
                int st = m.getState();
                if ((st != Module.CREATED) && (st != Module.STARTED)) {
                    if (first) {
                        first = false;
                        res.append(m.getModuleId());
                    } else {
                        res.append(',').append(m.getModuleId());
                    }
                }
            }
        }

        return res.toString();
    }

    /**
     * This class keeps deployed modules. Also it keeps track of
     * {@link ProvisionalModule}s count.
     */
    protected class DeployedModules<KeyType, ValueType> extends EnhancedMap<KeyType, ValueType> {

        /** Number of {@link ProvisionalModule}s */
        protected int unknownLoaderModules = 0;

        /** Empty constractor */
        public DeployedModules() {
        }

        /**
         * Returns get value
         * @param key key
         * @return value
         */
        public ValueType get(Object key) {
            if (key instanceof String) {
                String s = key.toString();
                Iterator<Map.Entry<KeyType, ValueType>> it = entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<KeyType, ValueType> entry = it.next();
                    if ((entry.getKey() != null) && s.equals(entry.getKey().toString())) {
                        return entry.getValue();
                    }
                }
                return null;
            } else {
                return super.get(key);
            }
        }

        /**
         * Adds new element to the map
         * @param key key
         * @param value value
         * @return previous value under that key
         */
        public ValueType put(KeyType key, ValueType value) {
            ValueType res = super.put(key, value);
            if ((value instanceof ProvisionalModule) && !(res instanceof ProvisionalModule)) {
                unknownLoaderModules = unknownLoaderModules + 1;
            }
            return res;
        }

        /**
         * Removes key from the map
         * @param key key
         * @return value under that key
         */
        public ValueType remove(Object key) {
            ValueType res = super.remove(key);
            if (res instanceof ProvisionalModule) {
                unknownLoaderModules = unknownLoaderModules - 1;
            }
            return res;
        }

        /**
         * Clears the map.
         */
        public void clear() {
            super.clear();
            unknownLoaderModules = 0;
        }

        /**
         * Returns the number of {@link ProvisionalModule}s
         * @return the number of {@link ProvisionalModule}s
         */
        public int getUnknownLoaderModulesCount() {
            return unknownLoaderModules;
        }
    }
}
