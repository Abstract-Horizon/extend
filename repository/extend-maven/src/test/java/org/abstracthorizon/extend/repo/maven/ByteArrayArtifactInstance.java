package org.abstracthorizon.extend.repo.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import org.abstracthorizon.extend.repo.AbstractArtifactInstance;
import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.Transport.FileStream;
import org.abstracthorizon.extend.repo.TransportObject.FileStreamImpl;

public class ByteArrayArtifactInstance extends AbstractArtifactInstance {

    private byte[] buffer;
    
    public ByteArrayArtifactInstance(byte[] buffer, Artifact artifact) {
        super(null, artifact);
        this.buffer = buffer;
    }
    
    @Override
    public long getLastModified() {
        return -1;
    }

    @Override
    public FileStream getStream() throws IOException {
        return new FileStreamImpl() {
            {
                setInputStream(new ByteArrayInputStream(buffer));
            }
            
            public long size() {
                return buffer.length;
            }
            
            public Date lastModified() {
                return null;
            }
        };
    }

}
