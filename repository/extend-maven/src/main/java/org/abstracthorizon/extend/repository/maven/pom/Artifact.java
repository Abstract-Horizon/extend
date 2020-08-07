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

import org.abstracthorizon.extend.server.deployment.ModuleId;

public class Artifact extends ModuleId {

    public Artifact() {
    }
    
    public Artifact(ModuleId copy) {
        super(copy);
    }
    
    public Artifact(Artifact copy) {
        super(copy);
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        if (groupId != null) {
            ToStringHelper.valueTag("groupId", groupId, res, indent);
        }
        if (artifactId != null) {
            ToStringHelper.valueTag("artifactId", artifactId, res, indent);
        }
        if (version != null) {
            ToStringHelper.valueTag("version", version, res, indent);
        }
        if (type != null) {
            ToStringHelper.valueTag("type", type, res, indent);
        }
        if (classifier != null) {
            ToStringHelper.valueTag("classifier", classifier, res, indent);
        }
        return res.toString();
    }
//
//    public String toString() {
//        return toString(0);
//    }

    public Artifact toPOMArtifact() {
        if ("pom".equals(type)) {
            return this;
        } else {
            Artifact pomArtifact = new Artifact(this);
            pomArtifact.setType("pom");
            pomArtifact.classifier = null;
            return pomArtifact;
        }
    }

    public boolean isSnapshot() {
        if ((version != null) && version.toUpperCase().endsWith("-SNAPSHOT")) {
            return true;
        }
        return false;
    }


    public Artifact toSnapshotArtifact() {
        if (version.toUpperCase().endsWith("-SNAPSHOT")) {
            return this;
        } else {
            Artifact snapshot = new Artifact(this);
            snapshot.setVersion(version + "-SNAPSHOT");
            return snapshot;
        }
    }

    public Artifact toNonSnapshotArtifact() {
        if (!version.toUpperCase().endsWith("-SNAPSHOT")) {
            return this;
        } else {
            Artifact notSnapshot = new Artifact(this);
            notSnapshot.setVersion(version.substring(0, version.length() - 9));
            return notSnapshot;
        }
    }

}
