/*
 * Copyright (c) 2008 Creative Sphere Limited.
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
package org.abstracthorizon.extend.support.spring.deployment;

/**
 * 
 * @author Daniel Sendula
 */
public class Dependency {
    
    private String value;
    
    private boolean uri = false;
    
    private boolean provided = false;
    
    private boolean optional = false;
    
    public Dependency() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isUri() {
        return uri;
    }

    public void setUri(boolean uri) {
        this.uri = uri;
    }

    public boolean isProvided() {
        return provided;
    }

    public void setProvided(boolean provided) {
        this.provided = provided;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String toString() {
        return "Dependency[" + value + ", isURI=" + isUri() + ", isProvided=" + isProvided() + ", isOptional=" + isOptional() + "]";
    }
}
