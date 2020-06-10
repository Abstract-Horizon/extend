package org.abstracthorizon.extend.repo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class FileTransport implements Transport {
    
    private File repository;
    
    public FileTransport(File repository) {
        this.repository = repository;
    }

    public Transport.FileStream open(String path) throws IOException {
        
        final File file = new File(repository, path);

        if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());

        return new TransportObject.FileStreamImpl() {
            {
                setInputStream(new FileInputStream(file));
            }
            public long size() { return file.length(); }
            
            public Date lastModified() { return new Date(file.lastModified()); }
        };
    }
        
    public void copy(Transport.FileStream inputStream, String path) throws IOException {
        File file = new java.io.File(repository, path);
            
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create dir " + dir.getAbsolutePath());
            }
        }

        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            byte[] buffer = new byte[10240];
            int r = inputStream.read(buffer);
            while ((r > 0) && !Thread.currentThread().isInterrupted()) {
                outputStream.write(buffer, 0, r);
                r = inputStream.read(buffer);
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        } catch (Throwable t) { 
            try {
                outputStream.close();
                file.delete();
            } catch (IOException ignore) {
            }
        } finally {
            try {
                outputStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    public void delete(String path) throws IOException {
        File file = new File(repository, path);
        if (!file.delete()) {
            throw new IOException("Cannot delete " + path);
        }
    }

    
}
