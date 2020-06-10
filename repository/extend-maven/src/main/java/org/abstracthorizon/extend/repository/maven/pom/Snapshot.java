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
 * Snapshot information
 *
 * @author Daniel Sendula
 */
public class Snapshot {

    protected String timestamp;

    protected String buildNumber;
    
    protected String localCopy;

    public Snapshot() {
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getLocalCopy() {
        return localCopy;
    }

    public void setLocalCopy(String localCopy) {
        this.localCopy = localCopy;
    }
    
    public boolean isLocalCopy() {
        return "true".equalsIgnoreCase(localCopy);
    }
}
