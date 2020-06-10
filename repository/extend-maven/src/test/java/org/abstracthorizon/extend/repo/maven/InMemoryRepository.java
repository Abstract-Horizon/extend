package org.abstracthorizon.extend.repo.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.abstracthorizon.extend.repo.Transport;
import org.abstracthorizon.extend.repo.TransportObject;

public class InMemoryRepository implements Transport {
    
    Map<String, byte[]> files = new HashMap<String, byte[]>();
    Map<String, Date> lastModifiedTimestamps = new HashMap<String, Date>();
    
    public void clear() {
        files.clear();
    }
    
    public Transport.FileStream open(final String path) throws FileNotFoundException {
        final byte[] array = files.get(path);
        if (array != null) {
            return new TransportObject.FileStreamImpl(new ByteArrayInputStream(array)) {
                public long size() { return array.length; }
                public Date lastModified() { return lastModifiedTimestamps.get(path); }
            };
        } else {
            throw new FileNotFoundException(path);
        }
    }
    
    public void copy(Transport.FileStream inputStream, String path) throws IOException {
        
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240];
        int r = inputStream.read(buffer);
        while (r > 0) {
            bodyStream.write(buffer, 0, r);
            r = inputStream.read(buffer);
        }
        inputStream.close();
        files.put(path, bodyStream.toByteArray());
    }

    public void delete(String path) {
        throw new UnsupportedOperationException();
    }
}
