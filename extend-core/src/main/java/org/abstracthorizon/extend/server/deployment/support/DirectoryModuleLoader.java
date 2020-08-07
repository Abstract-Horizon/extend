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
import java.net.URI;

import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.server.deployment.ModuleLoader;

/**
 * This module loader provides directories from the file system (URL with &quot;file:&quot; as a protocol)
 * that are without an extension.
 *
 * @author Daniel Sendula
 */
public class DirectoryModuleLoader implements ModuleLoader {

    /**
     * Empty constructor
     */
    public DirectoryModuleLoader() {
    }

    /**
    * Module loader for directories. It loads directories that do not have &quot;extension&quot;
    * (&quot;.&quot; in their name).
     */
    public boolean canLoad(URI uri) {
        String path = uri.getPath();
        if ("file".equals(uri.getScheme()) && (path != null) && path.endsWith("/")) {
            File file = new File(path);
            if (file.exists() && (file.getName().indexOf('.') < 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Translates URI to moduleId
     * @param uri uri
     * @return module id or <code>null</code>
     */
    public ModuleId toModuleId(URI uri) {
        String path = uri.getPath();
        return ModuleId.createModuleIdFromFileName(path);
    }

    /**
     * Creates {@link DirectoryModule} and returns it.
     * @param uri URI
     * @return initialised {@link DirectoryModule}
     */
    public Module load(URI uri) {
        return loadAs(uri, toModuleId(uri));
    }

    /**
     * Creates {@link DirectoryModule} and returns it.
     * @param uri URI
     * @param moduleId module id
     * @return initialised {@link DirectoryModule}
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        try {
            DirectoryModule service = new DirectoryModule(uri.toURL());
            service.setModuleId(moduleId);
            service.setState(Module.DEFINED);
            return service;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
