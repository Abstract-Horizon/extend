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
import java.util.Set;

import org.abstracthorizon.extend.server.support.EnhancedMap;

/**
 * This interface defines behaviour of main deployment manager.
 * It handles module loaders and deployment cycle of modules.
 *
 * @author Daniel Sendula
 */
public interface DeploymentManager extends ModuleLoader {

    /** Default name DeploymentManager can be found in a context */
    String DEPLOYMENT_MANAGER_DEFAULT_NAME = "DeploymentManager";

    /**
     * Deploys given module.
     * @param module module
     */
    void deploy(ModuleId moduleId, Module module);

    /**
     * Undeploys given module.
     * @param module module
     */
    void undeploy(Module module);

    /**
     * Re-deploys given module.
     * @param module module
     */
    void redeploy(Module module);

    /**
     * Calls {@link Module#create} method.
     * @param module module whose create method is to be called
     */
    void create(Module module);

    /**
     * Calls {@link Module#start} method.
     * @param module module whose start method is to be called
     */
    void start(Module module);

    /**
     * Calls {@link Module#stop} method.
     * @param module module whose stop method is to be called
     */
    void stop(Module module);

    /**
     * Calls {@link Module#destroy} method.
     * @param module module whose destroy method is to be called
     */
    void destroy(Module module);

    /**
     * This method loads module and deploys it before returning it to the caller.
     *
     * @param uri uri of the module as it is going to be passed to {@link ModuleLoader#load(URI)} method and {@link #deploy(URI, Module)} method
     * @return loaded module or <code>null</code> if module cannot be loaded
     * TODO check return statement
     */
    Module loadAndDeploy(URI uri);

    /**
     * Returns map of deployed modules.
     * @return map of deployed modules
     */
    EnhancedMap<ModuleId, Module> getDeployedModules();

    /**
     * Returns set of module loaders
     * @return module loaders
     */
    Set<ModuleLoader> getModuleLoaders();

    /**
     * Sets module loaders
     * @param moduleLoaders module loaders
     */
    void setModuleLoaders(Set<ModuleLoader> moduleLoaders);
}
