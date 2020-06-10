/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
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
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.support.KernelScheduler;
import org.abstracthorizon.extend.server.support.SimpleScheduler;

/**
 * This class scanns for list of URLs at intervals defined in scheduler
 * and checks if there are changes. In case of changes it removes, adds or redploys modules from given URLs.
 *
 * @author Daniel Sendula
 */
public class RedeployURLScanner implements Runnable {

    /** Reference to the deployment manager */
    protected DeploymentManager manager;

    /** Scheduler to check changes in this directory */
    protected SimpleScheduler scheduler;

    /** Map of URLs and URL details */
    protected Map<URL, URLDetails> map = new HashMap<URL, URLDetails>();

    /**
     * Constructor
     */
    public RedeployURLScanner() {
    }

    /**
     * Schedules running of this runnable object
     */
    public void init() {
        getSchedulerInternal().schedule(this);
    }

    /**
     * Removes this object from scheduler
     */
    public void destroy() {
        getSchedulerInternal().remove(this);
    }

    /**
     * Main method that scans for changes
     */
    public void run() {
        synchronized (map) {
            IdentityHashMap<Module, Module> redeployedModules = new IdentityHashMap<Module, Module>();
            Iterator<Map.Entry<URL, URLDetails>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<URL, URLDetails> entry = it.next();

                String protocol = entry.getKey().getProtocol();
                if ("file".equals(protocol)) {
                    try {
                        File file = new File(URLDecoder.decode(entry.getKey().getFile(), "UTF-8"));
                        if (file.exists()) {
                            URLDetails urlDetails = entry.getValue();
                            if (file.lastModified() != urlDetails.lastChagned) {
                                Module module = urlDetails.module;
                                if (redeployedModules.containsKey(module)) {
                                    // Prevent same module being redeployed because of multiple urls submitted
                                    urlDetails.lastChagned = file.lastModified();
                                } else {
                                    manager.redeploy(module);
                                    redeployedModules.put(module, module);
                                }
                            }
                        } else {
                            it.remove();
                        }
                    } catch (UnsupportedEncodingException ignore) {
                        throw new RuntimeException(ignore);
                    }
                }
            }
        }
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
     * Adds new URL to be scanned for changes
     * @param url url (currently must be of file protocol)
     * @param module moduel that is already loaded
     */
    public void addURL(URL url, Module module) {
        synchronized (map) {
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                try {
                    File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
                    URLDetails urlDetails = new URLDetails();
                    urlDetails.module = module;
                    urlDetails.lastChagned = file.lastModified();
                    map.put(url, null);
                } catch (UnsupportedEncodingException ignore) {
                    throw new RuntimeException(ignore);
                }
            } else {
                // throw new UnsupportedOperationException(url.getProtocol() + " is not supported!");
            }
        }
    }

    /**
     * Removes url
     * @param url url to be removed
     */
    public void removeURL(URL url) {
        synchronized (map) {
            map.remove(url);
        }
    }

    /**
     * Internal representation of URL details
     *
     * @author Daniel Sendula
     */
    protected static class URLDetails {
        /** Last changed */
        long lastChagned = -1;

        /** Reference to module */
        Module module;
    }
}
