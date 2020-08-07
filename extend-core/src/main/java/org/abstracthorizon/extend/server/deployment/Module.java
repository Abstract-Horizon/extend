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

import java.net.URL;
import java.util.Set;

/**
 * This interface describes a module that can be loaded and deployed.
 *
 * @author Daniel Sendula
 */
public interface Module {

    /** Undefined module */
    int UNDEFINED = 0;

    /** Module that is defined (loaded) */
    int DEFINED = 1;

    /** Module that waits of dependencies to be created */
    int WAITING_ON_CREATE = 2;

    /** Module that waits of dependencies to be created */
    int WAITING_ON_CREATE_TO_START = 3;

    /** Created module */
    int CREATED = 4;

    /** Waiting on modules this module depends to start */
    int WAITING_ON_START = 5;

    /** Deployed module */
    int STARTED = 6;

    /**
     * Returns module's original location.
     * @return module's original location
     */
    URL getOriginalLocation();

    /**
     * Returns module's working location
     * @return module's working location
     */
    URL getWorkingLocation();

    /**
     * Returns modules class loader.
     * @return modules class loader or <code>null</code>
     */
    ClassLoader getClassLoader();

    /**
     * Returns a set of modules that depend on this module.
     * @return a set of modules that depend on this module
     */
    Set<Module> getDependOnThis();

    /**
     * Returns a set of modules this module depends on.
     * @return a set of modules this module depends on
     */
    Set<Module> getDependsOn();

    /**
     * Sets the moduile's state.
     * <p>Note: Not to be called directly!</p>
     * @param state module's state
     */
    void setState(int state);

    /**
     * Returns module's state
     * @return module's state
     */
    int getState();

    /**
     * Return's module's name. This must not be null.
     * @return module's name
     */
    ModuleId getModuleId();

    /**
     * Creates the module.
     * <p>Note: Not to be called directly. Use {@link DeploymentManager#create(Module)} method instead.</p>
     */
    void create();

    /**
     * Starts the module.
     * <p>Note: Not to be called directly. Use {@link DeploymentManager#start(Module)} method instead.</p>
     */
    void start();

    /**
     * Stops the module.
     * <p>Note: Not to be called directly. Use {@link DeploymentManager#stop(Module)} method instead.</p>
     */
    void stop();

    /**
     * Destroys the module.
     * <p>Note: Not to be called directly. Use {@link DeploymentManager#destroy(Module)} method instead.</p>
     */
    void destroy();

}
