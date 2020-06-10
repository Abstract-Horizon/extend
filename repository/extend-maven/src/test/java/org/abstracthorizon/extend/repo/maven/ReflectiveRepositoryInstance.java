package org.abstracthorizon.extend.repo.maven;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.abstracthorizon.extend.repo.AbstractArtifactInstance;
import org.abstracthorizon.extend.repo.AbstractRepositoryInstanceImpl;
import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.Transport;
import org.abstracthorizon.extend.repo.TransportObject;

public class ReflectiveRepositoryInstance extends AbstractRepositoryInstanceImpl {
    
    public ReflectiveRepositoryInstance() throws URISyntaxException {
        super("destId", "Destination Id", new URI("destination://here"), true, true);
    }
    
    public ArtifactInstance find(Artifact artifact) {
        throw new RuntimeException(new FileNotFoundException(artifact.toString()));
    }
    
    public ArtifactInstance install(final ArtifactInstance artifactInstance) {
        return new AbstractArtifactInstance(artifactInstance.getRepository() /* This is to be able to read which repository we got it from! */, 
                                     artifactInstance.getArtifact()) {

            public long getLastModified() { return  -1; }
            
            public  Transport.FileStream getStream() {
                return new TransportObject.FileStreamImpl() {
                    {
                        setInputStream(new ByteArrayInputStream(artifactInstance.getArtifact().toString().getBytes()));
                    }
                    
                    public long size() { 
                        return artifactInstance.getArtifact().toString().length();
                    }
                    
                    public Date lastModified() { return null; }
                };
            }
        };
    }

}
