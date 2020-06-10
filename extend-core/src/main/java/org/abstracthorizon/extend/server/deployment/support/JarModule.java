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

import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.support.ClassUtils;


/**
 * Jar file module
 * 
 * @author Daniel Sendula
 */
public class JarModule extends AbstractModule {
    
    /**
     * Constructor
     * @param name name
     */
    public JarModule(ModuleId moduleId) {
        super(moduleId);
    }
    
    /**
     * This method creates the class loader
     */
    protected void createClassLoader() {
        ModuleClassLoader moduleClassLoader = new ModuleClassLoader(getClass().getClassLoader(), this);
        classLoader = ClassUtils.createClassLoader(moduleClassLoader, getWorkingLocation());
    }
    
    public String toString() {
        String n = super.toString();
        int i = n.lastIndexOf('/');
        if (i >= 0) {
            n = n.substring(i);
        }
        
        return "jar:" + n;
    }
}
