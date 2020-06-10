package org.abstracthorizon.extend.repo.maven;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.abstracthorizon.extend.repo.actors.Channel;
import org.abstracthorizon.extend.repo.maven.download.DownloadMessage;
import org.abstracthorizon.extend.repo.maven.download.DownloadResultMessage;

public class DownloadActors {
    
    private Channel<DownloadResultMessage> result = new Channel<DownloadResultMessage>();
    
    private Set<DownloadActor> actors = new LinkedHashSet<DownloadActor>();
    
    private Executor executor;
    
    public DownloadActors(Executor executor) {
        this.executor = executor;
    }
    
    protected Executor getExecutor() {
        if (executor == null) {
            executor = Executors.newCachedThreadPool();
        }
        return executor;
    }
    
    public void request(DownloadMessage inputMessage) {
        DownloadActor actor = new DownloadActor(this, inputMessage);
        synchronized (actors) {
            actors.add(actor);
        }
        getExecutor().execute(actor);
    }

    public Channel<DownloadResultMessage> getResultChannel() {
        return result;
    }
    
    protected void notifyFinished(DownloadActor actor) {
        synchronized (this) {
            actors.remove(actor);
        }
    }
    
    public void stopAll() {
        synchronized (this) {
            for (DownloadActor actor: actors) {
                actor.tryToStop();
            }
        }
    }
    
    public boolean isEmpty() {
        return actors.isEmpty() && !getResultChannel().hasResults();
    }

}
