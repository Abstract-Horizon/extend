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

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.service.AbstractServiceModuleLoader;

/**
 * Module loader that loads one jar only
 *
 * @author Daniel Sendula
 */
public class JarModuleLoader extends AbstractServiceModuleLoader {

    public JarModuleLoader() {
        extensions = new HashSet<String>(1);
        extensions.add(".jar");
    }
    
    /**
     * Translates URI to moduleId
     * @param uri uri
     * @return module id or <code>null</code>
     */
    public ModuleId toModuleId(URI uri) {
        String path = uri.getPath();
        if (path != null) {
            ModuleId moduleId = ModuleId.createModuleIdFromFileName(uri.getPath());
            moduleId.setArtifactId(path);
            return moduleId;
        } else {
            return null;
        }
    }

    /**
     * Loads Jar as a module
     * @param uri jar's URI
     * @return module
     */
    public Module load(URI uri) {
        ModuleId moduleId = toModuleId(uri);
        
        return loadAs(uri, moduleId);
    }
    
    /**
     * Loads Jar as a module
     * @param uri jar's URI
     * @return module
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        try {
            JarModule module = new JarModule(moduleId);
            module.setLocation(uri.toURL());
            module.create();
            module.start();
            return module;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
