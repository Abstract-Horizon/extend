package org.abstracthorizon.extend.repo;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class TransportObject {
    
    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static abstract class FileStreamImpl extends Transport.FileStream {

        private InputStream inputStream;
        
        public FileStreamImpl() {
        }

        public FileStreamImpl(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        
        protected void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        
        @Override 
        public int available() throws IOException { return inputStream.available(); }

        @Override 
        public void close() throws IOException { inputStream.close(); }
        
        @Override 
        public void mark(int readLimit) { inputStream.mark(readLimit); }

        @Override
        public boolean markSupported() { return inputStream.markSupported(); }
        
        @Override 
        public int read() throws IOException { return inputStream.read(); }

        @Override 
        public int read(byte[] b, int off, int len) throws IOException { return inputStream.read(b, off, len); }

        @Override 
        public void reset() throws IOException { inputStream.reset(); }
        
        @Override
        public long skip(long n) throws IOException { return inputStream.skip(n); }
    }

}
