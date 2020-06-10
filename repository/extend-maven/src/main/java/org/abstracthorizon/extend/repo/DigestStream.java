package org.abstracthorizon.extend.repo;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class DigestStream extends Transport.FileStream {

    private Transport.FileStream stream;
    private DigestInputStream digestStream;

    private MessageDigest hash;

    public DigestStream(Transport.FileStream stream) {
        this.stream = stream;
        try {
            hash = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digestStream = new DigestInputStream(stream, hash);
    }
    
    private String sha1HashValue = null;
    
    
    private boolean closed = false;
    
    
    public String sha1Hash() { 
        return sha1HashValue;
    }
    
    @Override
    public long size() {
        return stream.size();
    }

    @Override
    public Date lastModified() {
        return stream.lastModified();
    }
    
    protected void updateDigest() {
        byte[] sha1HashBytes = digestStream.getMessageDigest().digest();
        StringBuilder res = new StringBuilder();
        for (byte b : sha1HashBytes) {
            int i = (int)b;
            if (i < 0) {
                i = 256 + i;
            }
            String s = Integer.toHexString(i);
            if (s.length() != 2) {
                res.append('0');
            }
            res.append(s);
        }
        sha1HashValue = res.toString();
    }
    
    public int available() throws IOException { return digestStream.available(); }

    public void close() throws IOException {
        if (!closed) {
            digestStream.close();
            updateDigest();
            closed = true;
        }
    }
    
    @Override 
    public void mark(int readLimit) {
        digestStream.mark(readLimit);
    }
    
    @Override 
    public boolean markSupported() {
        return digestStream.markSupported();
    }

    @Override 
    public int read() throws IOException {
        return digestStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return digestStream.read(b, off, len);
    }

    @Override
    public void reset() throws IOException { digestStream.reset(); }

    @Override
    public long skip(long n)throws IOException { return digestStream.skip(n); }
            
    
}
