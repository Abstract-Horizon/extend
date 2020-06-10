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


public class Repository {

    protected String id;
    
    protected String name;
    
    protected String url;
    
    protected Releases releases = new Releases(true);
    
    protected Snapshots snapshots = new Snapshots(true);
    
    public Repository() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Snapshots getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(Snapshots snapshots) {
        this.snapshots = snapshots;
    }
    
    public Snapshots addSnapshots() {
        snapshots = new Snapshots();
        return snapshots;
    }

    public Releases getReleases() {
        return releases;
    }

    public Releases addReleases() {
        releases = new Releases();
        return releases;
    }

    public void setReleases(Releases releases) {
        this.releases = releases;
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        ToStringHelper.openTag("repository", res, indent);
        if (id != null) {
            ToStringHelper.valueTag("id", id, res, indent + 2);
        }
        if (url != null) {
            ToStringHelper.valueTag("url", url, res, indent + 2);
        }
        ToStringHelper.closeTag("repository", res, indent);
        return res.toString();
    }
    
    public String toString() {
        return toString(0);
    }
}
