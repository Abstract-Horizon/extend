package org.abstracthorizon.extend.repo.maven.download;

import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.RepositoryInstance;

public class Install extends DownloadMessage {
    
    private ArtifactInstance artifactInstance;
    private RepositoryInstance repository;
    
    public Install(ArtifactInstance artifactInstance, RepositoryInstance repository) {
        this.artifactInstance = artifactInstance;
        this.repository = repository;
    }

    public ArtifactInstance getArtifactInstance() {
        return artifactInstance;
    }

    public RepositoryInstance getRepository() {
        return repository;
    }
    
}
