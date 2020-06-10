package org.abstracthorizon.extend.repo;

import java.net.URI;

public abstract class AbstractRepositoryInstanceImpl extends RepositoryImpl implements RepositoryInstance {

    public AbstractRepositoryInstanceImpl() {
    }
    
    public AbstractRepositoryInstanceImpl(String id, String name, URI uri, boolean releases, boolean snapshots) {
        super(id, name, uri, releases, snapshots);
    }
    
    @Override
    public abstract ArtifactInstance find(Artifact artifact);

    @Override
    public abstract ArtifactInstance install(ArtifactInstance artifactInstance);

    @Override 
    public String toString() {
        return "RepositoryInstance[" + getId() + "," + getName() + "," + getURI() + "," + getReleases() + "," + getSnapshots() + "]";
    }

}
