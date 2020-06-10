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
package org.abstracthorizon.extend.server.deployment;

import java.net.URI;

/**
 *  This interface describes module loader - a class that knows how to load module from given URI.
 *
 * @author Daniel Sendula
 */
public interface ModuleLoader {

    /**
     * Returns <code>true</code> if module loader knows how to load (create) module from given URI.
     * @param uri URI
     * @return <code>true</code> if module loader knows how to load (create) module from given URI
     */
    boolean canLoad(URI uri);
    
    /**
     * Translates URI to moduleId
     * @param uri uri
     * @return module id or <code>null</code>
     */
    ModuleId toModuleId(URI uri);

    /**
     * Loads module from given URI. If {@link #canLoad(URI)} returns <code>false</code> then this method
     * will return <code>null</code>.
     * @param uri URI for module to be loaded from
     * @return module from given URI
     */
    Module load(URI uri);

    /**
     * Loads module from given URI. If {@link #canLoad(URI)} returns <code>false</code> then this method
     * will return <code>null</code>.
     * @param uri URI for module to be loaded from
     * @param moduleId module id to be used while creating module
     * @return module from given URI
     */
    Module loadAs(URI uri, ModuleId moduleId);

}
