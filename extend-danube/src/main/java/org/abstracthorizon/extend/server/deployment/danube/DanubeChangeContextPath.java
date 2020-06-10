/*
 * Copyright (c) 2009 Creative Sphere Limited.
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
package org.abstracthorizon.extend.server.deployment.danube;

import org.abstracthorizon.danube.http.Selector;
import org.abstracthorizon.danube.http.matcher.Matcher;
import org.abstracthorizon.danube.http.matcher.Prefix;
import org.abstracthorizon.extend.Extend;

/**
 * 
 * @author Daniel Sendula
 */
public class DanubeChangeContextPath {
    
    protected Selector selector;
    protected String fromPath;
    protected String toPath;
    protected boolean strict = false;
    
    public DanubeChangeContextPath() {
    }

    public DanubeChangeContextPath(Selector selector, String fromPath, String toPath) {
        setSelector(selector);
        setFromPath(fromPath);
        setToPath(toPath);
        init();
    }

    public void init() {
        for (Matcher matcher : selector.getComponents()) {
            if (matcher instanceof Prefix) {
                Prefix prefix = (Prefix)matcher;
                if (prefix.getPrefix().equals(fromPath)) {
                    prefix.setPrefix(toPath);
                    return;
                }
            }
        }
        if (strict) {
            throw new RuntimeException("Couldn't find Prefix matcher with path: " + fromPath);
        } else {
            Extend.info.info("Couldn't find Prefix matcher with path: " + fromPath);
        }
    }
    
    /**
     * @return the selector
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * @param selector the selector to set
     */
    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    /**
     * @return the fromPath
     */
    public String getFromPath() {
        return fromPath;
    }

    /**
     * @param fromPath the fromPath to set
     */
    public void setFromPath(String fromPath) {
        this.fromPath = fromPath;
    }

    /**
     * @return the toPath
     */
    public String getToPath() {
        return toPath;
    }

    /**
     * @param toPath the toPath to set
     */
    public void setToPath(String toPath) {
        this.toPath = toPath;
    }

    /**
     * Returns if it is strict - will it throw an exception in case of failure (or just log the error).
     * @return strict flag
     */
    public boolean isStrict() {
        return strict;
    }
    
    /**
     * Sets if it is strict - will it throw an exception in case of failure (or just log the error).
     * @param strict is strict
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
