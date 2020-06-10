package org.abstracthorizon.extend.repo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public interface Transport {

    FileStream open(String path) throws IOException;

    void copy(FileStream file, String path) throws IOException;

    void delete(String path) throws IOException;

    public abstract class FileStream extends InputStream {
        
        public abstract long size();

        public abstract Date lastModified();
    }
}
