package org.abstracthorizon.extend.repo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;

import org.abstracthorizon.extend.Extend;

public abstract class AbstractURLTransport implements Transport {


    protected abstract URL resourceURL(String path);
    
    public Transport.FileStream open(String path) throws IOException {
        URL url = resourceURL(path);
        try {
            if (Extend.transport.isDebugEnabled()) { Extend.transport.debug("Opening stream to " + url); }

            final URLConnection urlConnection = url.openConnection();
            final long sizeValue =  urlConnection.getContentLength();
            Date lastModified = null;
            if (urlConnection.getLastModified() >= 0) {
                lastModified = new Date(urlConnection.getLastModified());
            }
            final Date lastModifiedValue = lastModified;
                
            return new TransportObject.FileStreamImpl() {
                {
                    try {
                        setInputStream(urlConnection.getInputStream());
                    } catch (UnknownHostException e) {
                        throw new FileNotFoundException(e.getMessage());
                    }
                }
                
                public long size() { return sizeValue; }
                public Date lastModified() { return lastModifiedValue; }
            };
        } catch (IOException e) {
            throw e;
        }
    }
    
    @Override
    public void copy(Transport.FileStream inputStream, String path) throws IOException { throw new UnsupportedOperationException(); }

    @Override
    public void delete(String path) throws IOException { throw new UnsupportedOperationException(); }

}
