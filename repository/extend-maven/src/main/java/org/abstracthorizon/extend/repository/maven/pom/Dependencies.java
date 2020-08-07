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

import java.util.ArrayList;
import java.util.List;

public class Dependencies {

    protected List<Dependency> dependencies = new ArrayList<Dependency>();
    
    public Dependencies() {
    }
    
    public List<Dependency> getDependencies() {
        return dependencies;
    }
    
    public Dependency addDependency() {
        Dependency dependency = new Dependency();
        dependencies.add(dependency);
        return dependency;
    }
    
    public Dependency findDependency(String groupId, String artifactId) {
        for (Dependency dependency : dependencies) {
            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return dependency;
            }
        }
        return null;
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("dependencies", res, indent);
        for (Dependency dep : dependencies) {
            res.append(dep.toString(indent + 2));
        }
        ToStringHelper.closeTag("dependencies", res, indent);
        return res.toString();
    }

    public String toString() {
        return toString(2);
    }

}
