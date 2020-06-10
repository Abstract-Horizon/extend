package org.abstracthorizon.extend.repo.maven;

import java.io.IOException;
import java.io.InputStream;

import org.abstracthorizon.extend.repo.AbstractArtifactInstance;
import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.Transport;

public class MavenArtifactInstance extends AbstractArtifactInstance {

    private long timestamp = 0;
    private String buildNumber = null;
    private Transport.FileStream initialInputStream;
    private String artifactPath;
    private MavenRepository repository;
    
    public MavenArtifactInstance(MavenRepository repository, Artifact artifact, Transport.FileStream initialInputStream, String artifactPath) {
        super(repository, artifact);
        this.repository = repository;
        this.initialInputStream = initialInputStream;
        this.artifactPath = artifactPath;
    }
    
    public long getLastModified() {
        return timestamp;
    }
    
    public void setLastModified(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getBuildNumber() {
        return buildNumber;
    }
    
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }
    
    public Transport.FileStream getStream() throws IOException {
        if (initialInputStream != null) {
            Transport.FileStream res = initialInputStream;
            initialInputStream = null;
            return res;
        } else {
            return repository.getTransport().open(artifactPath);
        }
    }
    
    public InputStream getSHA1Stream() throws IOException {
        return repository.getTransport().open(artifactPath + ".sha1");
    }
}
