package org.abstracthorizon.extend.repo.maven;

import java.io.IOException;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.RepositoryInstance;
import org.abstracthorizon.extend.repo.maven.download.DownloadMessage;
import org.abstracthorizon.extend.repo.maven.download.DownloadResultMessage;
import org.abstracthorizon.extend.repo.maven.download.Find;
import org.abstracthorizon.extend.repo.maven.download.Found;
import org.abstracthorizon.extend.repo.maven.download.Install;
import org.abstracthorizon.extend.repo.maven.download.Installed;
import org.abstracthorizon.extend.repo.maven.download.NotFound;
import org.abstracthorizon.extend.repo.maven.download.NotInstalled;

public class DownloadActor implements Runnable {
    
    private DownloadActors ownerGroup;
    private Thread thread = null;
    private DownloadMessage message;
    
    public DownloadActor(DownloadActors ownerGroup, DownloadMessage message) {
        this.ownerGroup = ownerGroup;
        this.message = message;
    }
    
    protected DownloadMessage receive() {
        return message;
    }
    
    protected void reply(DownloadResultMessage result) {
        ownerGroup.getResultChannel().send(result);
    }
    
    public void run() {
        act();
    }

    public void act() {
        thread = Thread.currentThread();
        DownloadMessage work = receive();
        
        if (work instanceof Find) {
            Find find = (Find)work;
            RepositoryInstance sourceRepository = find.getRepository();
            Artifact artifact = find.getArtifact();
            try {
                ArtifactInstance artifactInstance = sourceRepository.find(artifact);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                if (artifactInstance != null) {
                    reply(new Found(artifactInstance));
                } else {
                    reply(new NotFound(artifact, sourceRepository, null));
                }
            } catch (InterruptedException i) {
                reply(new NotFound(artifact, sourceRepository, i));
            } catch (IOException e) {
                reply(new NotFound(artifact, sourceRepository, e));
            } catch (Throwable t) {
                reply(new NotFound(artifact, sourceRepository, t));
            }
        } else if (work instanceof Install) {
            Install install = (Install)work;
            RepositoryInstance repository = install.getRepository();
            ArtifactInstance artifactInstance = install.getArtifactInstance();
            try {
                ArtifactInstance resultArtifactInstance = repository.install(artifactInstance);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                if (resultArtifactInstance != null) {
                    reply(new Installed(resultArtifactInstance));
                } else {
                    reply(new NotInstalled(artifactInstance, null));
                }
            } catch (InterruptedException i) {
                reply(new NotInstalled(artifactInstance, i));
            } catch (IOException e) {
                reply(new NotInstalled(artifactInstance, e));
            } catch (Throwable t) {
                reply(new NotInstalled(artifactInstance, t));
            }
        } else {
            System.err.println("ERROR: Received unknown message!" + work);
            System.exit(1);
        }
        ownerGroup.notifyFinished(this);
    }

    public void tryToStop() {
        thread.interrupt();
    }

}
