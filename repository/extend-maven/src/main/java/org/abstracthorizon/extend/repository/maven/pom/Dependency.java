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


public class Dependency extends Artifact {

    protected String scope;
    
    protected Exclusions exclusions;
    
    protected String optional;
    
    public Dependency() {
    }

    public Exclusions getExclusions() {
        return exclusions;
    }
    
    public Exclusions addExclusions() {
        exclusions = new Exclusions();
        return exclusions;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getOptional() {
        return optional;
    }
    
    public boolean isOptional() {
        return "true".equalsIgnoreCase(optional);
    }

    public void setOptional(String optional) {
        this.optional = optional;
    }

//    public List<Dependency> filterDependencies() {
//        ArrayList<Dependency> result = new ArrayList<Dependency>();
//        result.addAll(getDependencies());
//        if (exclusions != null) {
//            Iterator<Dependency> it = result.iterator();
//            while (it.hasNext()) {
//                Dependency d = it.next();
//                if (exclusions.contains(d)) {
//                    it.remove();
//                }
//            }
//        }
//        
//        return result;
//    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("dependency", res, indent);
        res.append(super.toString(indent + 2));
        if (scope != null) {
            ToStringHelper.valueTag("scope", scope, res, indent + 2);
        }
        if (optional != null) {
            ToStringHelper.valueTag("optional", optional, res, indent + 2);
        }
        if (exclusions != null) {
            res.append(exclusions.toString(indent + 2));
        }
        ToStringHelper.closeTag("dependency", res, indent);
        return res.toString();
    }
    
    public String toString() {
        return toString(0);
    }
}
