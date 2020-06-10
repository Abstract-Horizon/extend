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

import java.util.ArrayList;
import java.util.List;

public class Exclusions {

    protected List<Exclusion> exclusions = new ArrayList<Exclusion>();
    
    public Exclusions() {
    }

    public List<Exclusion> getExclusions() {
        return exclusions;
    }
    
    public Exclusion addExclusion() {
        Exclusion exclusion = new Exclusion();
        exclusions.add(exclusion);
        return exclusion;
    }
     
    public boolean contains(Artifact dependency) {
        for (Exclusion e : getExclusions()) {
            if (dependency.getGroupId().equals(e.getGroupId()) && dependency.getArtifactId().equals(e.getArtifactId())) {
                // TODO is this enough?
                return true;
            }
        }
        return false;
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("exclusions", res, indent);
        for (Exclusion exclusion : exclusions) {
            res.append(exclusion.toString(indent + 2));
        }
        ToStringHelper.closeTag("exclusions", res, indent);
        return res.toString();
    }

    public String toString() {
        return toString(2);
    }

}
