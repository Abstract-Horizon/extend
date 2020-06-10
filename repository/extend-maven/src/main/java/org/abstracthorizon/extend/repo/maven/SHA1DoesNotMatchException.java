package org.abstracthorizon.extend.repo.maven;

import java.io.IOException;

public class SHA1DoesNotMatchException extends IOException {
    
    public SHA1DoesNotMatchException(String msg) {
        super(msg);
    }

}
