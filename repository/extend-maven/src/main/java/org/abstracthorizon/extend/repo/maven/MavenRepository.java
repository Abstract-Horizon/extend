package org.abstracthorizon.extend.repo.maven;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactImpl;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.RepositoryImpl;
import org.abstracthorizon.extend.repo.RepositoryInstance;
import org.abstracthorizon.extend.repo.Transport;
import org.abstracthorizon.extend.repo.TransportObject;
import org.abstracthorizon.extend.repo.util.XMLUtils;
import org.w3c.dom.Document;

public class MavenRepository extends RepositoryImpl implements RepositoryInstance {

    private boolean local;
    private Transport transport;
    private MavenRepositoryFactory mavenRepositoryFactory;
    
    protected MavenRepository(
            String id, 
            String name, 
            URI uri, 
            boolean releases, 
            boolean snapshots,
            boolean local,
            Transport transport,
            MavenRepositoryFactory mavenRepositoryFactory) { 
        
        super(id, name, uri, releases, snapshots) ;
        this.mavenRepositoryFactory = mavenRepositoryFactory;
                
        this.local = local;
        this.transport = transport;
    }

    protected boolean isLocal() {
        return local;
    }
    
    public Transport getTransport() {
        return transport;
    }
    
    protected String installArtifactPath(Artifact artifact, String artifactFullPath) throws IOException {
        if (local) {
            return artifactFullPath;
        } else if (artifact.getVersion().isFinal()) {
            throw new IOException("Installing to remote repository " + this + " not yet implemented");
        } else {
            throw new IOException("Installing to remote repository " + this + " not yet implemented");
        }
    }

    public ArtifactInstance find(final Artifact artifact) throws IOException {

        if (artifact.getVersion().isFinal() && !getReleases()) {
            throw new FileNotFoundException("Repository doesn't hold releases");
        }
        
        if (!artifact.getVersion().isFinal() && !getSnapshots()) {
            throw new FileNotFoundException("Repository doesn't hold snapshots");
        }
        
        final LocalRecord localRecord = new LocalRecord();
        

        String artifactFullPath = mavenRepositoryFactory.defaultArtifactPath(artifact);
        final String artifactDirPath = mavenRepositoryFactory.defaultArtifactDir(artifact);
        // String artifactName = MavenRepositoryFactory.defaultArtifactName(artifact);
        
        // TODO now implement checking if file is correct (local repository), snapshot versions etc...
        
        if (artifact.getVersion().isFinal()) {
            localRecord.artifactPath = artifactFullPath;
            localRecord.firstInputStream = transport.open(localRecord.artifactPath);
            if (localRecord.firstInputStream.lastModified() != null) {
                localRecord.timestamp = localRecord.firstInputStream.lastModified().getTime();
            }
//            } else if (local) {
//                artifactPath = artifactFullPath
//                firstInputStream = transport.open(artifactPath)
        } else {
            // Snapshot
            String metadataXMLFileName = artifactDirPath + "maven-metadata.xml";

            try {
                Transport.FileStream metadataXMLInputStream = transport.open(metadataXMLFileName);
                MavenArtifactInstance metadataArtifactInstance = new MavenArtifactInstance(this, ArtifactImpl.apply(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "maven-metadata.xml"), 
                        metadataXMLInputStream, metadataXMLFileName);
    
                try {
                    WithSHA withSHA1 = new WithSHA();
                    
                    withSHA1.check(metadataArtifactInstance, new WithSHA.StreamBody() {
                        public void body(Transport.FileStream artifactStream) throws IOException {
                            Document xml = XMLUtils.load(artifactStream);
                            
                            String timestampString = XMLUtils.getLastValue(xml, "timestamp");
                            try {
                                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd.HHmmss");
                                localRecord.timestamp = format.parse(timestampString).getTime();
                            } catch (java.text.ParseException ignore) {
                            }
                            
                            localRecord.buildNumber = XMLUtils.getLastValue(xml, "buildNumber");
                            String artifactName = mavenRepositoryFactory.artifactName(artifact, timestampString, localRecord.buildNumber);
                            localRecord.artifactPath = artifactDirPath + artifactName;
                            localRecord.firstInputStream = transport.open(localRecord.artifactPath);
                        }
                    });
                } catch (SHA1DoesNotMatchException x) {
                    localRecord.artifactPath = artifactFullPath;
                    localRecord.firstInputStream = transport.open(localRecord.artifactPath);
                    if (localRecord.firstInputStream.lastModified() != null) {
                        localRecord.timestamp = localRecord.firstInputStream.lastModified().getTime();
                    }
                } catch (Exception x) {
                    reThrow(x);
                }
            } catch (FileNotFoundException x) {
                // Remote repository, snapshot and no 'maven-metadata.xml' present. Try just snapshot jar itself!

                localRecord.artifactPath = artifactFullPath;
                localRecord.firstInputStream = transport.open(localRecord.artifactPath);
                if (localRecord.firstInputStream.lastModified() != null) {
                    localRecord.timestamp = localRecord.firstInputStream.lastModified().getTime();
                }
            } catch (Exception x) {
                reThrow(x);
            }
        }
        
        
        MavenArtifactInstance res = new MavenArtifactInstance(this, artifact, localRecord.firstInputStream, localRecord.artifactPath);
        res.setLastModified(localRecord.timestamp);
        res.setBuildNumber(localRecord.buildNumber);
        return res;
    }

    private static class LocalRecord {
        String buildNumber = null;
        long timestamp = 0;
        
        Transport.FileStream firstInputStream = null;
        String artifactPath = null;
    }

    
    public ArtifactInstance install(ArtifactInstance artifactInstance) throws IOException {
            
        Artifact artifact = artifactInstance.getArtifact();
        
        String artifactFullPath = mavenRepositoryFactory.defaultArtifactPath(artifact);
        // String artifactDirPath = MavenRepositoryFactory.defaultArtifactDir(artifact);
        // String artifactName = MavenRepositoryFactory.defaultArtifactName(artifact);
        
        // TODO now implement checking if file is correct (local repository), snapshot versions etc...
        final String artifactPath = installArtifactPath(artifact, artifactFullPath);

        try {
            WithSHA withSHA1 = new WithSHA();
            
            withSHA1.check(artifactInstance, new WithSHA.StreamBody() {
                public void body(Transport.FileStream artifactStream) throws IOException {
                    transport.copy(artifactStream, artifactPath);
                }
    
            });
            
            withSHA1.withCalculatedSHA1(new WithSHA.SHA1Body() {
                
                @Override
                public void body(final String sha1) throws IOException {
                    transport.copy(new TransportObject.FileStreamImpl(new ByteArrayInputStream(sha1.getBytes())) {

                        @Override public long size() { return sha1.length(); }
                        
                        @Override public Date lastModified() { return null; }
                        
                    }, artifactPath + ".sha1");
            }});
        } catch (SHA1DoesNotMatchException x) {
            try {
                transport.delete(artifactPath);
            } catch (IOException ignore) { }
            try {
                transport.delete(artifactPath + ".sha1");
            } catch (IOException ignore) { }
            throw x;
        } catch (Exception x) {
            if (x instanceof RuntimeException) {
                throw (RuntimeException)x;
            } else {
                throw new RuntimeException(x);
            }
        }
        
        MavenArtifactInstance res = new MavenArtifactInstance(this, artifact, null, artifactPath);
        return res;
    }
    
    //    private boolean checkSha1(MavenArtifactInstance m) {
    //        return true;
    //    }

    public static void reThrow(Exception x) {
        if (x instanceof RuntimeException) { 
            throw (RuntimeException)x;
        } else { 
            throw new RuntimeException(x); 
        }
    }
}
