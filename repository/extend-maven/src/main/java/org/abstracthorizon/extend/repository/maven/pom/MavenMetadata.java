/*
 * Copyright (c) 2007 Creative Sphere Limited.
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
package org.abstracthorizon.extend.repository.maven.pom;

/**
 * Maven metadata file descriptor
 *
 * @author Daniel Sendula
 */
public class MavenMetadata {

    protected Versioning versioning;

    public MavenMetadata() {
    }

    public Versioning getVersioning() {
        return versioning;
    }

    public Versioning addVersioning() {
        if (versioning == null) {
            versioning = new Versioning();
        }
        return versioning;
    }

}
