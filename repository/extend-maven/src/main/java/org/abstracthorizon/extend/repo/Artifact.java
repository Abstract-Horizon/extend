package org.abstracthorizon.extend.repo;

public interface Artifact {
    
    String getGroupId();
    
    String getArtifactId();

    Version getVersion();
    
    String getType();
    
    String getClassifier();

}
