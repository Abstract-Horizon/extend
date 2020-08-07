/*
 * Copyright (c) 2007-2020 Creative Sphere Limited.
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
 * Versioning
 *
 * @author Daniel Sendula
 */
public class Versioning {

    protected String lastUpdated;
    
    protected Snapshot snapshot;

    public Versioning() {
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public Snapshot addSnapshot() {
        snapshot = new Snapshot();
        return snapshot;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
