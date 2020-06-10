package org.abstracthorizon.extend.repo.maven.download;

import org.abstracthorizon.extend.repo.ArtifactInstance;

public class Found extends DownloadResultMessage {

    private ArtifactInstance artifactInstance;
    
    public Found(ArtifactInstance artifactInstance) {
        this.artifactInstance = artifactInstance;
    }

    public ArtifactInstance getArtifactInstance() {
        return artifactInstance;
    }

}
