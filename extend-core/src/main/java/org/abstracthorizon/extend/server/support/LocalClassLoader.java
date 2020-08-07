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
package org.abstracthorizon.extend.server.support;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Class loader need to implement this in order to avoid delegation process (through parent class loader).
 *
 * @author Daniel Sendula
 */
public interface LocalClassLoader {

    Class<?> loadLocalClass(String name) throws ClassNotFoundException;

    URL getLocalResource(String name);

    Enumeration<URL> getLocalResources(String name) throws IOException;
}
