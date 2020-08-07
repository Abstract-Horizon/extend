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

public class Exclusion extends Artifact {

    public Exclusion() {
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("exclusion", res, indent);
        res.append(super.toString(indent + 2));
        ToStringHelper.closeTag("exclusion", res, indent);
        return res.toString();
    }
    
    public String toString() {
        return toString(0);
    }
}
