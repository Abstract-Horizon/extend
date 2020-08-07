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

public class ToStringHelper {

    public static String indent(int i) {
        if (i > 0) {
            return "                                                                              ".substring(0, i);
        } else {
            return "";
        }
    }
    
    public static void indent(StringBuffer buf, int indent) {
        for (int i = 0; i < indent; i++) {
            buf.append(' ');
        }
    }
    
    public static void openTag(String name, StringBuffer buf, int indent) {
        for (int i = 0; i < indent; i++) {
            buf.append(' ');
        }
        buf.append('<').append(name).append('>');
        buf.append('\n');
    }
    
    public static void closeTag(String name, StringBuffer buf, int indent) {
        for (int i = 0; i < indent; i++) {
            buf.append(' ');
        }
        buf.append('<').append('\\').append(name).append('>');
        buf.append('\n');
    }
    
    public static void valueTag(String name, String value, StringBuffer buf, int indent) {
        for (int i = 0; i < indent; i++) {
            buf.append(' ');
        }
        buf.append('<').append(name).append('>');
        buf.append(value);
        buf.append('<').append('\\').append(name).append('>');
        buf.append('\n');
    }
    
}
