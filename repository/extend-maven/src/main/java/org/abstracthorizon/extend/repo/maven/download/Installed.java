package org.abstracthorizon.extend.repo.maven.download;

import org.abstracthorizon.extend.repo.ArtifactInstance;

public class Installed extends DownloadResultMessage {

    private ArtifactInstance artifactInstance;
    
    public Installed(ArtifactInstance artifactInstance) {
        this.artifactInstance = artifactInstance;
    }

    public ArtifactInstance getArtifactInstance() {
        return artifactInstance;
    }
    
}
