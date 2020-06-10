package org.abstracthorizon.extend.repo.maven;

import java.util.Set;

import org.abstracthorizon.extend.repo.Artifact;

public interface Dependency extends Artifact {

    Set<Artifact> getExclusions();
    
    String getScope();
    
    boolean isOptional();
    
    boolean isPOM();
    
}
