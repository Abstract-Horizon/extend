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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;


/**
 * Jar file module
 * 
 * @author Daniel Sendula
 */
public abstract class AbstractModule implements Module {

    /** Module's class loader */
    protected ClassLoader classLoader;
    
    /** List of modules this module depends on */
    protected Set<Module> dependsOn = new HashSet<Module>();
    
    /** List of modules depending on this module */
    protected Set<Module> dependOnThis = new HashSet<Module>();
    
    /** Real location */
    protected URL workingUrl;
    
    /** Location it was originally expected on */
    protected URL originalUrl;
    
    /** Module's name */
    protected ModuleId moduleId;

    /** State */
    protected int state = UNDEFINED;

    /**
     * Caution - use with care as every module <b>must</b> have
     * module id defined!
     */
    protected AbstractModule() {
    }
    
    /**
     * Constructor
     * @param moduleId module id
     */
    public AbstractModule(ModuleId moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Sets the location
     * @param url url of the module
     */
    public void setLocation(URL url) {
        this.workingUrl = url;
        this.originalUrl = url;
    }
    
    /**
     * Creates the module. It sets up the classloader
     */
    public void create() {
        createClassLoader();
        setState(CREATED);
    }

    /**
     * This method does nothing
     */
    public void start() {
        setState(STARTED);
    }

    /**
     * This method does nothing
     */
    public void stop() {
        setState(CREATED);
    }

    /**
     * Destroys the module. It removes classloader 
     */
    public void destroy() {
        classLoader = null;
        setState(DEFINED);
    }

    /**
     * Returns module's classloader
     * @return classloader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns a set of modules that depend on this 
     * @return a set of modules that depend on this
     */
    public Set<Module> getDependOnThis() {
        return dependOnThis;
    }

    /**
     * Returns a set of modules this module depends on 
     * @return a set of modules this module depends on
     */
    public Set<Module> getDependsOn() {
        return dependsOn;
    }

    /**
     * Returns module's module id
     * @return module id
     */
    public ModuleId getModuleId() {
        return moduleId;
    }
    
    /**
     * Sets module's module id
     * @param moduleId module id
     */
    public void setModuleId(ModuleId moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Returns original location
     * @return original location
     */
    public URL getOriginalLocation() {
        return originalUrl;
    }

    /**
     * Returns real location
     * @return real location
     */
    public URL getWorkingLocation() {
        return workingUrl;
    }

    /**
     * Returns state
     * @return state
     */
    public int getState() {
        return state;
    }

    /**
     * Changes the state
     * @param state state
     */
    public void setState(int state) {
        this.state = state;
    }
    
    /**
     * This method creates the class loader. Current implementation does nothing.
     */
    protected void createClassLoader() {
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
        return moduleId.toString();
    }
}
