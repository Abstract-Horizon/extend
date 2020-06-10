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
package org.abstracthorizon.extend.repository.maven;

import java.io.File;
import java.net.URL;
import java.util.Stack;

import org.abstracthorizon.extend.repository.maven.pom.Artifact;

/**
 * Definitions describing download task. 
 *
 * @author Daniel Sendula
 */
public class DownloadTaskDefinitions {

    private ParallelRepositoryModuleLoader loader;
    private URL url;
    private boolean snapshot;
    private File destinationFile;
    private Stack<Artifact> stack;
    
    public DownloadTaskDefinitions(ParallelRepositoryModuleLoader loader, URL url, boolean snapshot, File destinationFile, Stack<Artifact> stack) {
        this.loader = loader;
        this.url = url;
        this.snapshot = snapshot;
        this.destinationFile = destinationFile;
        this.stack = stack;
    }
    
    public String toString() {
        return "DownloadTaskDefs[" + url + "," + destinationFile + "]";
    }
    
    public boolean equals(Object o) {
        if (o instanceof DownloadTaskDefinitions) {
            DownloadTaskDefinitions other = (DownloadTaskDefinitions)o;
            if (url == other.url) {
                return true;
            }
            if (url != null) {
                return url.equals(other.url);
            }
        }
        return super.equals(o);
    }
    
    public int hashCode() {
        if (url != null) {
            return url.hashCode();
        } else {
            return 0;
        }
    }

    public URL getURL() {
        return url;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public File getDestinationFile() {
        return destinationFile;
    }

    public Stack<Artifact> getStack() {
        return stack;
    }

    public ParallelRepositoryModuleLoader getLoader() {
        return loader;
    }
}
