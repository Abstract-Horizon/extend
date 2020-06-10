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

import java.util.HashMap;
import java.util.Map;

public class Properties extends HashMap<String, String> {

    public Properties() {
    }

    
    public String toString() {
        return toString(0);
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("properties", res, indent);
        for (Map.Entry<String, String> entry : entrySet()) {
            ToStringHelper.valueTag(entry.getKey(), entry.getValue(), res, indent + 2);
        }
        ToStringHelper.closeTag("properties", res, indent);
        return res.toString();
    }
}
