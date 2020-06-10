package org.abstracthorizon.extend.repo.maven.download;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.RepositoryInstance;

public class NotFound extends DownloadResultMessage {

    private Artifact artifact;
    private RepositoryInstance repositoryInstance;
    private Throwable reason;
    
    public NotFound(Artifact artifact, RepositoryInstance repositoryInstance, Throwable reason) {
        this.artifact = artifact;
        this.repositoryInstance = repositoryInstance;
        this.reason = reason;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public RepositoryInstance getRepositoryInstance() {
        return repositoryInstance;
    }

    public Throwable getReason() {
        return reason;
    }
    
}
