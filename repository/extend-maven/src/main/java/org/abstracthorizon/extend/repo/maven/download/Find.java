package org.abstracthorizon.extend.repo.maven.download;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.RepositoryInstance;

public class Find extends DownloadMessage {
    
    private Artifact artifact;
    private RepositoryInstance repository;
    
    public Find(Artifact artifact, RepositoryInstance repository) {
        this.artifact = artifact;
        this.repository = repository;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public RepositoryInstance getRepository() {
        return repository;
    }

}