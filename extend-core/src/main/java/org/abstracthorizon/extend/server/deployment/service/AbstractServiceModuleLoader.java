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
package org.abstracthorizon.extend.server.deployment.service;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;

/**
 * Service module loader. It loads &quot;service.xml&quot; file as
 * spring's application context xml configuration file.
 * TODO: explain donwloading and unpacking of archives
 *
 * @author Daniel Sendula
 */
public abstract class AbstractServiceModuleLoader implements ModuleLoader /*, ApplicationContextAware */{

    /** Set of extensions loader will work on */
    protected Set<String> extensions;
    
    /** Deployment manager */
    protected DeploymentManager deploymentManager;

    /**
     * Empty constructor
     */
    public AbstractServiceModuleLoader() {
    }
    
    /**
     * Sets deployment manager 
     * @param deploymentManager deployment manager
     */
    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }
    
    /**
     * Returns deployment manager
     * @return deployment manager
     */
    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    /**
     * Returns <code>true</code> if module loader knows how to load (create) module from given URI.
     * @param uri URI
     * @return <code>true</code> if module loader knows how to load (create) module from given URI.
     */
    public boolean canLoad(URI uri) {
        String file = uri.getPath();
        if (file != null) { 
            if (file.endsWith("/")) {
                if ("file".equals(uri.getScheme())) {
                    file = file.substring(0, file.length() - 1);
                } else {
                    return false;
                }
            }
            if (extensions != null) {
                for (String extension : extensions) {
                    if (file.endsWith(extension)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns set of extensions this module will work with.
     * @return set of extensions this module will work with
     */
    public Set<String> getExtensions() {
        if (extensions == null) {
            extensions = new HashSet<String>();
        }
        return extensions;
    }

    /**
     * Sets set of extensions this module will work with.
     * @param extensions set of extensions this module will work with
     */
    public void setExtensions(Set<String> extensions) {
        this.extensions = extensions;
    }


    /**
     * Adds this loader to &quot;DeploymentManager&quot; {@link DeploymentManager}.
     */
    public void start() {
        deploymentManager.getModuleLoaders().add(this);
    }

    /**
     * Removes this loader from &quot;DeploymentManager&quot; {@link DeploymentManager}.
     */
    public void stop() {
        deploymentManager.getModuleLoaders().remove(this);
    }
}
