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

import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.DeploymentManagerImpl;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleUtils;

/**
 * This class is used as a placeholder for a module that
 * is to be deployed in near future. It is needed for {@link DeploymentManagerImpl}.
 *
 * @author Daniel Sendula
 */
public class ProvisionalModule implements Module {

    /** URI **/
    protected URI uri;

    /** Modules name */
    protected ModuleId moduleId;

    /** List of modules that depend on this module */
    protected LinkedHashSet<Module> dependOnThis = new LinkedHashSet<Module>();

    /**
     * Constructor. It sets name as last part of url's file
     * @param uri URI it came from so it can be tried to be re-deployed
     * @param moduleId module id
     */
    public ProvisionalModule(URI uri, ModuleId moduleId) {
        this.uri = uri;
        this.moduleId = moduleId;
    }

    /**
     * Returns <code>null</code>
     * @return <code>null</code>
     */
    public ClassLoader getClassLoader() {
        return null;
    }

    /**
     * Returns a list of modules that depend on this module
     * @return a list of modules that depend on this module
     */
    public Set<Module> getDependOnThis() {
        return dependOnThis;
    }

    /**
     * Returns an empty set
     * @return an empty set
     */
    public Set<Module> getDependsOn() {
        return ModuleUtils.EMPTY_SET;
    }

    /**
     * Returns <code>null</code>
     * @return <code>null</code>
     */
    public URL getOriginalLocation() {
        return null;
    }

    /**
     * Returns <code>null</code>
     * @return <code>null</code>
     */
    public URL getWorkingLocation() {
        return null;
    }

    /**
     * Returns module's id
     * @return module's id
     */
    public ModuleId getModuleId() {
        return moduleId;
    }

    /**
     * @throws RuntimeException
     */
    public void create() {
//        throw new RuntimeException("Cannot be created");
    }

    /**
     * @throws RuntimeException
     */
    public void start() {
//        throw new RuntimeException("Cannot be deployed");
    }

    /**
     * @throws RuntimeException
     */
    public void stop() {
//        throw new RuntimeException("Cannot be undeployed");
    }

    /**
     * Empty implementation
     */
    public void destroy() {
    }

    public void setState(int state) {
        throw new RuntimeException("State cannot be changed");
    }

    /**
     * Returns {@link Module#UNDEFINED}
     * @return {@link Module#UNDEFINED}
     */
    public int getState() {
        return Module.UNDEFINED;
    }

    /**
     * Returns {@link Module#UNDEFINED} as string
     * @return {@link Module#UNDEFINED} as string
     */
    public String getStateAsString() {
        return ModuleUtils.stateAsString(Module.UNDEFINED);
    }
    
    /**
     * Returns uri
     * @return uri
     */
    public URI getURI() {
        return uri;
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
    
    public String toString() {
        URI uri = getURI();
        if (uri != null) {
            return "ProvisionalModule[" + getModuleId() + ", " + getURI() + "]";
        } else {
            return "ProvisionalModule[" + getModuleId() + "]";
        }
    }
}
