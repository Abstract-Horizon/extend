package org.abstracthorizon.extend.repo;

public interface Version {

    boolean isFinal();
            
    boolean matches(Version version);
    
    Version toFinal();

}
