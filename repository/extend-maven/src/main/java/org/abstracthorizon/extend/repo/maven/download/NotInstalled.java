package org.abstracthorizon.extend.repo.maven.download;

import org.abstracthorizon.extend.repo.ArtifactInstance;

public class NotInstalled extends DownloadResultMessage {

    private ArtifactInstance artifactInstance;
    private Throwable reason;
    
    public NotInstalled(ArtifactInstance artifactInstance, Throwable reason) {
        this.artifactInstance = artifactInstance;
        this.reason = reason;
    }

    public ArtifactInstance getArtifactInstance() {
        return artifactInstance;
    }

    public Throwable getReason() {
        return reason;
    }
    
}
