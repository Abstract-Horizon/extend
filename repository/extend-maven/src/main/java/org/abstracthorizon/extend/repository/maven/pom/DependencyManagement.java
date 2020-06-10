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

public class DependencyManagement {

    protected Dependencies dependencies;
    
    public DependencyManagement() {
        
    }
    
    public Dependencies getDependencies() {
        return dependencies;
    }


    public Dependencies addDependencies() {
        if (dependencies == null) {
            dependencies = new Dependencies();
        }
        return dependencies;
    }

    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        if (dependencies != null) {
            ToStringHelper.openTag("dependencyManagement", res, indent);
            res.append(dependencies.toString(indent + 2));
            ToStringHelper.closeTag("dependencyManagement", res, indent);
        }
        return res.toString();
    }

    public String toString() {
        return toString(0);
    }
}
