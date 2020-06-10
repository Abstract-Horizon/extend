package org.abstracthorizon.extend.repo.maven;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.DigestStream;
import org.abstracthorizon.extend.repo.Transport;

public class WithSHA {

    private String localSha1;

    public WithSHA() {

    }
    
    public void check(ArtifactInstance artifactInstance, StreamBody body) throws SHA1DoesNotMatchException, IOException {
        Transport.FileStream artifactStream = artifactInstance.getStream();
        DigestStream digestArtifactStream = new DigestStream(artifactStream);
        try {
            body.body(digestArtifactStream);
        } finally {
            digestArtifactStream.close();
        }
        
        String remoteSha1 = null;
        try {
            if (artifactInstance instanceof MavenArtifactInstance) {
                MavenArtifactInstance m = (MavenArtifactInstance)artifactInstance;
                InputStream sha1Stream = m.getSHA1Stream();
                try {
                    String sha1 = readSha1(sha1Stream);
                    remoteSha1 = sha1;
                } finally {
                    sha1Stream.close();
                }
            }
        } catch (FileNotFoundException x) {
        } catch (IOException x) { 
            throw x; 
        }

        localSha1 = digestArtifactStream.sha1Hash();
        if (remoteSha1 != null) {
            if (!localSha1.equals(remoteSha1)) {
                Extend.transport.debug("Sha1s do not match " + remoteSha1 + " != " + localSha1 + "; " + artifactInstance.getArtifact());
                throw new SHA1DoesNotMatchException("Sha1s do not match " + remoteSha1 + " != " + localSha1 + "; " + artifactInstance.getArtifact());
            } else {
                Extend.transport.debug("Verified SHA1 digest of " + artifactInstance.getArtifact());
            }
        } else {
            Extend.transport.debug("Artifact doesn't have SHA1 digest; " + artifactInstance.getArtifact());
        }
    }
    
    public void withCalculatedSHA1(SHA1Body body)  throws IOException {
        body.body(localSha1);
    }
    
    public static String readSha1(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        if (line != null) {
            int i = line.indexOf(' ');
            if (i > 0) {
                return line.substring(0, i);
            } else if (line.length() > 0) {
                return line;
            }
        }
        return null;
    }
    
    public static interface StreamBody {
        void body(Transport.FileStream artifactStream) throws IOException;
    }

    public static interface SHA1Body {
        void body(String sha1) throws IOException;
    }
    
}
