package org.abstracthorizon.extend.repo.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactImpl;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.FileTransport;
import org.abstracthorizon.extend.repo.Repository;
import org.abstracthorizon.extend.repo.RepositoryInstance;
import org.abstracthorizon.extend.repo.Transport;
import org.abstracthorizon.extend.repo.TransportFactory;
import org.abstracthorizon.extend.repo.URLTransportFactory;
import org.abstracthorizon.extend.repo.Version;
import org.abstracthorizon.extend.repo.maven.download.DownloadResultMessage;
import org.abstracthorizon.extend.repo.maven.download.Find;
import org.abstracthorizon.extend.repo.maven.download.Found;
import org.abstracthorizon.extend.repo.maven.download.Install;
import org.abstracthorizon.extend.repo.maven.download.Installed;
import org.abstracthorizon.extend.repo.maven.download.NotFound;
import org.abstracthorizon.extend.repo.maven.download.NotInstalled;

public class MavenRepositoryFactory {
    
    public List<RepositoryInstance> repositories = new ArrayList<RepositoryInstance>();
    public MavenRepository localRepository;

    public TransportFactory defaultTransportFactory = new URLTransportFactory();

    private Executor executor = Executors.newCachedThreadPool();

    
    public MavenRepositoryFactory() {
        localRepository = localRepository("local", "Local repository", 
                new File(new File(System.getProperty("user.home")), ".m2/repository"), true, true);
    }
    
    public MavenRepository apply(String id, 
            String name,
            URI uri,
            boolean releases,
            boolean snapshots,
            boolean local,
            Transport transport) {
        return new MavenRepository(id, name, uri, releases, snapshots, local, transport, this) ;
    }
    
    public MavenRepository apply(String id, 
            String name, 
            URI uri, 
            boolean releases, 
            boolean snapshots,
            boolean local,
            TransportFactory transportFactory) {
        return new MavenRepository(id, name, uri, releases, snapshots, local, transportFactory.transport(uri), this);
    }
    
    public MavenRepository apply(String id, 
            String name, 
            URI uri, 
            boolean releases, 
            boolean snapshots,
            boolean local) {
        return apply(id, name, uri, releases, snapshots, local, defaultTransportFactory);
    }
    
    public MavenRepository apply(Repository repository) {
        return apply(repository.getId(), repository.getName(), repository.getURI(), repository.getReleases(), false, repository.getSnapshots(), defaultTransportFactory);
    }
    public MavenRepository apply(Repository repository, Transport transport) {
        return apply(repository.getId(), repository.getName(), repository.getURI(), repository.getReleases(), repository.getSnapshots(), false, transport);
    }
    public MavenRepository apply(Repository repository, TransportFactory transportFactory) {
        return apply(repository.getId(), repository.getName(), repository.getURI(), repository.getReleases(), repository.getSnapshots(), false, transportFactory);
    }
    
    public String defaultArtifactPath(Artifact artifact) {
        return defaultArtifactDir(artifact) + defaultArtifactName(artifact);
    }

    public String defaultArtifactDir(Artifact artifact) {
        return artifact.getGroupId().replace('.', '/') + '/' + artifact.getArtifactId() + '/' +  artifact.getVersion().toString() + '/';
    }

    public String defaultArtifactName(Artifact artifact) {
        StringBuilder res = new StringBuilder();
        res.append(artifact.getArtifactId());
        res.append('-');
        res.append(artifact.getVersion().toString());
        if (artifact.getClassifier() != null) {
            res.append('-');
            res.append(artifact.getClassifier());
        }
        res.append('.').append(artifact.getType());
        return res.toString();
    }

    public String artifactName(Artifact artifact, String timestamp, String buildNumber) {
        StringBuilder res = new StringBuilder();
        res.append(artifact.getArtifactId());
        res.append('-');
        res.append(artifact.getVersion().toFinal().toString());
        res.append('-');
        res.append(timestamp);
        res.append('-');
        res.append(buildNumber);
        if (artifact.getClassifier() != null) {
            res.append('-');
            res.append(artifact.getClassifier());
        }
        res.append('.').append(artifact.getType());
        return res.toString();
    }

    public ArtifactInstance find(Artifact artifact) {
        return find(artifact, repositories);
    }
    
    public ArtifactInstance find(Artifact artifact, Collection<RepositoryInstance> repositories) {
        DownloadActors downloaders = startFind(artifact, repositories);

        while (!downloaders.isEmpty()) {
            DownloadResultMessage reply = downloaders.getResultChannel().receive();

            if (reply instanceof Found) {
                Found found = (Found)reply;
                downloaders.stopAll();
                return found.getArtifactInstance();
            } else if (reply instanceof NotFound) {
                NotFound notFound = (NotFound)reply;
                if (notFound.getReason() instanceof FileNotFoundException) {
                    Extend.transport.debug("Not found " + notFound.getArtifact() + " @ " + notFound.getRepositoryInstance(), notFound.getReason().getMessage());
                } else {
                    Extend.transport.debug("Not found " + notFound.getArtifact() + " @ " + notFound.getRepositoryInstance(), notFound.getReason());
                }
            } else {
                throw new RuntimeException("Got strange message from some of DownloadAgents " + reply);
            }
        }
        return null;
    }
    
    public ArtifactInstance ensureInLocal(Artifact artifact) throws FileNotFoundException {
        return ensureIn(artifact, repositories, localRepository);
    }
    
    /**
     * This method checks if artifact is already in the repository and if not downloads it form the
     * fastest (and not failing) repository from the list of repositories.
     * 
     * @param artifact artifact to be resolved
     * @param repositories repositories to be used in {@link ensureIn()} method to ensure resulted artifact instances are in the repository
     * @param destination destination repository
     * @return artifact instance in destination repository of asked artifact or throws FileNotFoundException if artifact cannot be found
     *         or other IOException if it cannot be downloaded
     */
    public ArtifactInstance ensureIn(Artifact artifact, 
            Collection<RepositoryInstance> repositories, 
            RepositoryInstance destination) throws FileNotFoundException {

        boolean isSnapshot = !artifact.getVersion().isFinal();
        List<ArtifactInstance> snapshotInstances = null;
        if (!artifact.getVersion().isFinal()) { snapshotInstances = new ArrayList<ArtifactInstance>(); }
        
        try {
            ArtifactInstance localArtifact = destination.find(artifact);
            if (isSnapshot) {
                snapshotInstances.add(localArtifact);
            } else {
                return localArtifact;
            }
        } catch (IOException notFound) { 
        }
        
        
        DownloadActors downloaders = startFind(artifact, repositories);

        List<ArtifactInstance> foundInstances = new ArrayList<ArtifactInstance>();
        ArtifactInstance current = null;
        

        while (!downloaders.isEmpty()) {
            DownloadResultMessage reply = downloaders.getResultChannel().receive();
            if (reply instanceof Found) {
                Found found = (Found)reply;
                ArtifactInstance artifactInstance = found.getArtifactInstance();
                if (isSnapshot) {
                    snapshotInstances.add(artifactInstance);
                } else {
                    if (current == null) {
                        startDownload(downloaders, artifactInstance, destination);
                    } else {
                        foundInstances.add(artifactInstance);
                    }
                }
            } else if (reply instanceof Installed) {
                Installed installed = (Installed)reply;
                downloaders.stopAll();
                return installed.getArtifactInstance();
            } else if (reply instanceof NotFound) {
                NotFound notFound = (NotFound)reply;
                if (notFound.getReason() instanceof FileNotFoundException) {
                    Extend.transport.debug("Not found " + notFound.getArtifact() + " @ " + notFound.getRepositoryInstance(), notFound.getReason().getMessage());
                } else {
                    Extend.transport.debug("Not found " + notFound.getArtifact() + " @ " + notFound.getRepositoryInstance(), notFound.getReason());
                }
            } else if (reply instanceof NotInstalled) {
                //NotInstalled notInstalled = (NotInstalled)reply;
                synchronized(this) {
                    current = null;
                    if (foundInstances.size() > 0) {
                        current = foundInstances.get(0);
                        foundInstances.remove(0);
                    }
                    if (current != null) {
                        startDownload(downloaders, current, destination);
                    }
                }
            } else {
                throw new RuntimeException("Got strange message from some of DownloadAgents " + reply);
            }
        }
        
        if (isSnapshot) {
            List<ArtifactInstance> sortedSnapshotInstances = new ArrayList<ArtifactInstance>(snapshotInstances);
            Collections.sort(sortedSnapshotInstances, 
                new Comparator<ArtifactInstance>() {

                    @Override
                    public int compare(ArtifactInstance a, ArtifactInstance b) {
                        if (a.getLastModified() > b.getLastModified()) {
                            return 0;
                        } else if (a.getLastModified() > b.getLastModified()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
            });
            
            
            for (ArtifactInstance artifactInstance : sortedSnapshotInstances) {
                startDownload(downloaders, artifactInstance, destination);
                while (!downloaders.isEmpty()) {
                    DownloadResultMessage reply = downloaders.getResultChannel().receive();
                    if (reply instanceof Found) {
                    } else if (reply instanceof NotFound) {
                    } else if (reply instanceof Installed) {
                        Installed installed = (Installed)reply;
                        downloaders.stopAll();
                        return installed.getArtifactInstance();
                    } else if (reply instanceof NotInstalled) {
                    } else {
                        throw new RuntimeException("Got strange message from some of DownloadAgents " + reply);
                    }
                }
            }
        }
        throw new FileNotFoundException("Cannot resolve " + artifact + " in " + repositoriesToString(repositories));
    }
    
    protected void startDownload(DownloadActors downloadActors, ArtifactInstance artifactInstance, RepositoryInstance destination) {
        Install install = new Install(artifactInstance, destination);
        downloadActors.request(install);
    }

    /**
     * <p>This method resolves asked artifact to destination repository. It will ensure that all dependencies of
     * asked artifact are resolved first. Resolving involves finding (obtaining) corresponding POM of the artifact
     * and then resolving all dependencies of the POM. As process is recursive all transitive dependencies are
     * resolved as well. This method ensures that all dependencies are used with callback function before
     * asked artifact.</p>
     * 
     * <p>This method throws FileNotFoundException if any of dependencies or asked artifact cannot be find or
     * other IOException if it cannot be downloaded.</p>
     * 
     * @param artifact artifact to be resolved
     * @param repositories repositories to be used in {@link ensureIn()} method to ensure resulted artifact instances are in the repository
     * @param destination destination repository
     * @param callback callback function that will be called for all dependencies (excluding POMs) and at then asked artifact.
     */
    public void resolveTo(Artifact artifact, 
            Collection<RepositoryInstance> repositories, 
            RepositoryInstance destination,
            ResolutionCallback resolutionCallback) throws FileNotFoundException {
     
        Dependency d;
        if (artifact instanceof Dependency) {
            d = (Dependency)artifact;
        } else {
            d = new SimpleDependency(artifact, null, false);
        }

        localResolveToWithSnapshots(d, repositories, new ResolutionContext(destination, resolutionCallback));
    }
    
    protected void localResolveToWithSnapshots(
            Dependency dependency, 
            Collection<RepositoryInstance> repositories, 
            ResolutionContext context) throws FileNotFoundException {
        localResolveTo(dependency, repositories, context);
    }
    
    protected void localResolveTo(
            Dependency dependency, 
            Collection<RepositoryInstance> repositories, 
            ResolutionContext context) throws FileNotFoundException {

        Artifact mainArtifact = null;
        if (dependency.getType() == null) {
            mainArtifact = ArtifactImpl.apply(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), "jar", dependency.getClassifier());
        } else {
            mainArtifact = dependency;
        }

        if (mainArtifact instanceof Dependency) {
            context.stack.push(ArtifactImpl.apply(mainArtifact));
        } else {
            context.stack.push(mainArtifact);
        }
        
        try {
            Extend.transport.debug("Resolving " + dependency + " from " + repositoriesToString(repositories) + " to " + context.destination);
    
            ArtifactInstance finalArtifact = null;
            if (!dependency.isPOM()) {
                try {
                    finalArtifact = ensureIn(mainArtifact, repositories, context.destination);
                    context.alreadyHandled.add(ArtifactImpl.apply(mainArtifact));
                } catch (FileNotFoundException ioe) { 
                    if (mainArtifact.getVersion().isFinal()) {
                        // It is final, so we will check if snapshot is available
                        mainArtifact = MavenArtifact.toSnapshot(mainArtifact);
                        try {
                            finalArtifact = ensureIn(mainArtifact, repositories, context.destination);
                            context.alreadyHandled.add(ArtifactImpl.apply(mainArtifact));
                        } catch (FileNotFoundException ioe2) {
                            if (!dependency.isOptional()) { 
                                throw ioe2;
                            } else {
                                return;
                            }
                        }
                    } else {
                        throw ioe;
                    }
                }
            }
            
            
            Artifact pomArtifact = MavenArtifact.toPOM(mainArtifact);
            try {
                ArtifactInstance pomArtifactInstance = ensureIn(pomArtifact, repositories, context.destination);
                resolvePOM(pomArtifactInstance, repositories, context);
            } catch (FileNotFoundException notFound) {
                    if (dependency.isPOM() && !dependency.isOptional()) {
                        throw notFound;
                    }
                    Extend.transport.debug("No POM available " + pomArtifact);
            } catch (Throwable t) { 
                    Extend.transport.warn("Failed to load pom " + pomArtifact, t);
                    throw new RuntimeException(t);
            }
            if (!dependency.isPOM()) {
                context.resolutionCallback.finished(finalArtifact);
            }
        } finally {
            context.stack.pop();
        }
    }
    
    protected void resolvePOM(ArtifactInstance pomArtifactInstance, 
            Collection<RepositoryInstance> repositories, 
            ResolutionContext context) throws IOException {
        MavenPOM pom = MavenPOM.load(pomArtifactInstance);
        Collection<Repository> pomRepos = pom.getRepositories();
        Collection<RepositoryInstance> reps = repositories;
        
        if (pomRepos.size() != 0) {
            LinkedHashSet<RepositoryInstance> r = new LinkedHashSet<RepositoryInstance>();
            r.addAll(toRepositoryInstances(pomRepos));
            r.addAll(repositories);

            reps = r;
        }
        
        if ((pom.getParentArtifact() != null) && (pom.getParentPOM() == null)) {
            MavenPOM parentPOM = context.poms.get(pom.getParentArtifact());
            if (parentPOM != null) { 
                pom.updateParentPOM(parentPOM);
            } else {
                localResolveTo(new SimpleDependency(MavenArtifact.toPOM(pom.getParentArtifact()), null, false), reps, context);
                parentPOM = context.poms.get(pom.getParentArtifact());
                if (parentPOM != null) {
                    pom.updateParentPOM(parentPOM);
                } else {
                    throw new FileNotFoundException("Couldn't resolve parent pom " + pom.getParentArtifact());
                }
            }
        }
        
        context.poms.put(ArtifactImpl.baseVersionedArtifact(pom.getArtifact()), pom);
        
        for (Dependency pomsDep : pom.getDependencies()) {
            Artifact artifact;
            if (pomsDep.getType() == null) {
                artifact = ArtifactImpl.fallbackToType(pomsDep, "jar");
            } else {
                artifact = ArtifactImpl.apply(pomsDep);
            }
            if (context.stack.contains(artifact)) {
                throw new IOException("Got to circular reference: " + pomsDep + " is being resolved. Stack " + artifactsToString(context.stack));
            }
            if (context.resolutionCallback.control(artifact) && !context.alreadyHandled.contains(artifact)) {
                localResolveToWithSnapshots(pomsDep, reps, context);
            }
        }
    }
    
    protected DownloadActors startFind(Artifact artifact, Collection<RepositoryInstance> repositories) {
        DownloadActors downloadActors = new DownloadActors(executor);
        
        for (RepositoryInstance repository : repositories) {
            Find request = new Find(artifact, repository);
            downloadActors.request(request);
        }

        return downloadActors;
    }
    
    protected static String repositoriesToString(Collection<RepositoryInstance> t) {
        StringBuilder res = new StringBuilder();

        boolean first = true;
        for (RepositoryInstance r : t) {
            if (first) { first = false; } else { res.append(", "); }
            res.append("Repository[").append(r.getName()).append('(').append(r.getId()).append("),");
            res.append(r.getURI()).append(']');
        }
        
        return res.toString();
    }

    protected static String artifactsToString(Collection<Artifact> t) {
        StringBuilder res = new StringBuilder();

        res.append('[');
        boolean first = true;
        for (Artifact r : t) {
            if (first) { first = false; } else { res.append(", "); }
            res.append(r.toString());
        }
        res.append(']');
        return res.toString();
    }

    protected List<RepositoryInstance> toRepositoryInstances(Collection<Repository> t) {
        List<RepositoryInstance> res = new ArrayList<RepositoryInstance>();
        for (Repository r : t) {
            if (r instanceof RepositoryInstance) {
                res.add((RepositoryInstance)r);
            } else {
                res.add(apply(r));
            }
        }
        return res;
    }

    // val ALWAYS_ACCEPT = { a: Artifact => true }
    
    // def RESOLVE_DEEP(finishedCallback: ArtifactInstance => Unit) = {
    //     artifactInstance: ArtifactInstance =>
    //         finishedCallback(artifactInstance);
    // }

    public MavenRepository localRepository(String id, String name, File repositoryDir, boolean releases, boolean snapshots) {
        return new MavenRepository(id, name, repositoryDir.toURI(), releases, snapshots, true, new FileTransport(repositoryDir), this);
    }
    
    public static interface ResolutionCallback {
        boolean control(Artifact artifact);
        void finished(ArtifactInstance artifactInstance);
    }

    public static class ResolutionContext {
        
        RepositoryInstance destination;
        ResolutionCallback resolutionCallback;
        
        protected ResolutionContext(RepositoryInstance destination, 
                ResolutionCallback resolutionCallback) {
            this.destination = destination;
            this.resolutionCallback = resolutionCallback;
        }
        
        Map<Artifact, MavenPOM> poms = new HashMap<Artifact, MavenPOM>();
        Stack<Artifact> stack = new Stack<Artifact>();
        Set<Artifact> alreadyHandled = new HashSet<Artifact>();
    }

    static class SimpleDependency implements Dependency {
        
        private Artifact artifact;
        private String scope;
        private boolean optional;
        
        protected SimpleDependency(Artifact artifact, String scope, boolean optional) {
            this.artifact = artifact;
            this.scope = scope;
            this.optional = optional;
        }
        
        @Override
        public String getGroupId() {
            return artifact.getGroupId();
        }

        @Override
        public String getArtifactId() {
            return artifact.getArtifactId();
        }
        
        @Override
        public Version getVersion() {
            return artifact.getVersion();
        }

        @Override
        public String getType() {
            return artifact.getType();
        }

        @Override
        public String getClassifier() {
            return artifact.getClassifier();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<Artifact> getExclusions() {
            return Collections.EMPTY_SET;
        }

        @Override
        public String getScope() {
            return scope;
        }

        @Override
        public boolean isOptional() {
            return optional;
        }

        @Override
        public boolean isPOM() {
            return "pom".equals(getType());
        }
        
        public String toString() {
            return "Dependecy[" + artifact + ", scope=" + scope + ", optional=" + optional + "]";
        }
    }

}


