package org.abstracthorizon.extend.repo;

import java.net.URI;

public interface Repository {
    
    String getId();
    
    String getName();
    
    URI getURI();
    
    boolean getReleases();

    boolean getSnapshots();
}
