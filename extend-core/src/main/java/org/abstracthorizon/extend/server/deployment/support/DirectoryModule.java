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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import org.abstracthorizon.extend.server.deployment.ModuleId;

/**
 * This module represents a directory. It doesn't perform any task but making supplied directory
 * available as a module in a list of modules.
 *
 * @see org.abstracthorizon.extend.support.spring.deployment.AbstractApplicationContextModule
 *
 * @author Daniel Sendula
 */
public class DirectoryModule extends AbstractModule {

    protected File dir;
    
    /**
     * Empty constructor
     * @throws MalformedURLException 
     */
    public DirectoryModule(URL url) throws MalformedURLException {
        super(ModuleId.createModuleIdFromFileName(new File(url.getPath())));
        setLocation(url);
        if (!"file".equals(url.getProtocol())) {
            throw new RuntimeException("Only file protocol is supported.");
        }
        dir = new File(getWorkingLocation().getPath());
    }

    /**
     * Empty implementation
     */
    protected void createInternal() {
    }

    /**
     * Empty implementation
     */
    protected void startInternal() {
    }

    /**
     * Empty implementation
     */
    protected void stopInternal() {
    }

    /**
     * Empty implementation
     */
    protected void destroyInternal() {
    }
    
    
    /**
     * This method creates the class loader
     */
    protected void createClassLoader() {
        classLoader = new ClassLoader() {            
            public URL findResource(String name) {
                File f = new File(dir, name);
                if (f.exists()) {
                    try {
                        return f.toURI().toURL();
                    } catch (MalformedURLException ignore) {
                    }
                }
                return null;
            }

            public Enumeration<URL> findResources(String name) {
                URL url = findResource(name);
                Vector<URL> v = new Vector<URL>();
                if (url != null) {
                    v.add(url);
                }
                return v.elements();
            }
        };
    }
}
