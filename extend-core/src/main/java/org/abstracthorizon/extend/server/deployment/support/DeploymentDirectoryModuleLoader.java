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
 * This module loader accepts only directories on the filesystem (file://) and
 * returns {@link DeploymentDirectoryModule}. It also uses non singleton definition
 * of {@link DeploymentDirectoryModule} from the supplied application context.
 *
 * @author Daniel Sendula
 */
public class DeploymentDirectoryModuleLoader implements ModuleLoader {

    /**
     * Empty constructor
     */
    public DeploymentDirectoryModuleLoader() {
    }

    /**
     * Returns <code>true</code> if URI represens directory (ends with &quot;/&quot;) and
     * doesn't have &quot;.&quot; in the name.
     * @param uri URI
     * @return <code>true</code> if URI represens directory (ends with &quot;/&quot;) and
     * doesn't have &quot;.&quot; in the name
     */
    public boolean canLoad(URI uri) {
        String path = uri.getPath();
        if ("file".equals(uri.getScheme()) && (path != null) && path.endsWith("/")) {
            File file = new File(path);
            if (file.exists()) {
                // Deploy only existing directories that do not nave extension or are 'invisible' (starting with a dot)
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
        if (path != null) {
            ModuleId moduleId = new ModuleId();
            moduleId.setArtifactId(path);
            return moduleId;
        } else {
            return null;
        }
    }
    
    /**
     * Obtains bean named &quot;DirectoryModule&quot; ({@link DeploymentDirectoryModule}) from the application context,
     * sets location and name and returns it.
     * @param uri URI for module to be loaded from
     * @return initialised {@link DeploymentDirectoryModule}
     */
    public Module load(URI uri) {
        ModuleId moduleId = toModuleId(uri);
        return loadAs(uri, moduleId);
    }


    /**
     * Obtains bean named &quot;DirectoryModule&quot; ({@link DeploymentDirectoryModule}) from the application context,
     * sets location and name and returns it.
     * @param uri URI for module to be loaded from
     * @param moduleId module id
     * @return initialised {@link DeploymentDirectoryModule}
     */
    public Module loadAs(URI uri, ModuleId moduleId) {
        try {
            DeploymentDirectoryModule path = new DeploymentDirectoryModule();
            File file = new File(uri.getPath());
            moduleId.setArtifactId(file.getName());
            path.setModuleId(moduleId);
            path.setLocation(uri.toURL());
            path.setState(Module.DEFINED);
            return path;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        
    }
}
