package org.abstracthorizon.extend.repo;

import java.io.IOException;

public interface ArtifactInstance {

    long getLastModified();
    
    Artifact getArtifact();
    
    Repository getRepository();
    
    Transport.FileStream getStream()  throws IOException;

}
