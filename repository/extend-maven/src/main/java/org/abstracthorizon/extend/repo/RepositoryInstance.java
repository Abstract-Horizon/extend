package org.abstracthorizon.extend.repo;

import java.io.IOException;

public interface RepositoryInstance extends Repository {
    
    ArtifactInstance find(Artifact artifact) throws IOException;
    
    ArtifactInstance install(ArtifactInstance artifactInstance) throws IOException;

}
