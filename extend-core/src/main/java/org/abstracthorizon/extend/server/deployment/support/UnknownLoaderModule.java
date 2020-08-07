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
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.DeploymentManagerImpl;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleUtils;

/**
 * This represents module of an URL that doesn't have
 * known module loader
 *
 * @see DeploymentManager
 * @see DeploymentManagerImpl
 *
 * @author Daniel Sendula
 */
public class UnknownLoaderModule implements Module {

    /** URI */
    protected URI uri;

    /** Modules name */
    protected ModuleId moduleId;

    /**
     * Constructor. It sets name as last part of url's file
     * @param uri URI location
     */
    public UnknownLoaderModule(URI uri) {
        this.uri = uri;
        this.moduleId = new ModuleId();
        moduleId.setGroupId("not_deployed");
        moduleId.setArtifactId(uri.toString());
    }

    /**
     * Returns <code>null</code>
     * @return <code>null</code>
     */
    public ClassLoader getClassLoader() {
        return null;
    }

    /**
     * Returns an empty set
     * @return an empty set
     */
    public Set<Module> getDependOnThis() {
        return ModuleUtils.EMPTY_SET;
    }

    /**
     * Returns an empty set
     * @return an empty set
     */
    public Set<Module> getDependsOn() {
        return ModuleUtils.EMPTY_SET;
    }

    /**
     * Returns module's location
     * @return module's location
     */
    public URL getOriginalLocation() {
        return null;
    }

    /**
     * Returns module's location
     * @return module's location
     */
    public URL getWorkingLocation() {
        return null;
    }

    /**
     * Returns URI
     * @return
     */
    public URI getURI() {
        return uri;
    }
    
    /**
     * Returns name as </code>&quot;_not_deployed_&quot; + location.toString()</code>;
     * @return name as </code>&quot;_not_deployed_&quot; + location.toString()</code>;
     */
    public ModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Empty implementation
     */
    public void create() {
    }

    /**
     * @throws RuntimeException
     */
    public void start() {
        throw new RuntimeException("Cannot be deployed");
    }

    /**
     * @throws RuntimeException
     */
    public void stop() {
        throw new RuntimeException("Cannot be undeployed");
    }

    /**
     * Empty implementation
     */
    public void destroy() {
    }

    /**
     * @throws RuntimeException
     */
    public void setState(int state) {
        throw new RuntimeException("State cannot be changed");
    }

    /**
     * Returns {@link Module#STARTED}
     * @return {@link Module#STARTED}
     */
    public int getState() {
        return Module.STARTED;
    }

    /**
     * Returns {@link Module#STARTED} as string
     * @return {@link Module#STARTED} as string
     */
    public String getStateAsString() {
        return ModuleUtils.stateAsString(Module.STARTED);
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
