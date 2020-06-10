package org.abstracthorizon.extend.repo;

import java.net.URI;

public class RepositoryImpl implements Repository {

    private String id;
    private String name;
    private URI uri;
    private boolean releases;
    private boolean snapshots;

    public RepositoryImpl() {
       // Remove this
    }
    
    public RepositoryImpl(String id, String name, URI uri, boolean releases, boolean snapshots) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.releases = releases;
        this.snapshots = snapshots;
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public boolean getReleases() {
        return releases;
    }

    @Override
    public boolean getSnapshots() {
        return snapshots;
    }

    @Override
    public String toString() {
        return "Repository[" + getId() + "," + getName() + "," + getURI() + "," + getReleases() + "," + getSnapshots() + "]";
    }

}
