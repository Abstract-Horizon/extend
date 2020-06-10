package org.abstracthorizon.extend.repository.maven

import scala.actors.!
import scala.actors.Actor
import scala.actors.Channel
import scala.actors.InputChannel
import scala.actors.Future
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.LinkedHashSet
import scala.collection.mutable.Stack
import scala.util.Sorting
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.XML 

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.IOException
import java.io.FileNotFoundException
import java.io.OutputStream

import java.net.URI
import java.net.URL
import java.net.HttpURLConnection


import org.abstracthorizon.extend.repository._

object MavenRepository extends Log {
    
    val repositories = new ArrayBuffer[RepositoryInstance]
    val localRepository = new LocalMavenRepository("local", "Local repository", new File(new File(System.getProperty("user.home")), ".m2/repository"), true, true)

    var defaultTransportFactory: TransportFactory = URLTransportFactory
    
    def apply(id: String, 
        name: String, 
        uri: URI, 
        releases: Boolean, 
        snapshots: Boolean,
        local: Boolean,
        transport: Transport): MavenRepository = new MavenRepository(id, name, uri, releases, snapshots, local, transport)
    
    def apply(id: String, 
        name: String, 
        uri: URI, 
        releases: Boolean, 
        snapshots: Boolean,
        local: Boolean,
        transportFactory: TransportFactory): MavenRepository = new MavenRepository(id, name, uri, releases, snapshots, local, transportFactory.transport(uri))
    
    def apply(id: String, 
        name: String, 
        uri: URI, 
        releases: Boolean, 
        snapshots: Boolean,
        local: Boolean
        ): MavenRepository = apply(id, name, uri, releases, snapshots, local, defaultTransportFactory)
    
    def apply(repository: Repository): MavenRepository = apply(repository.id, repository.name, repository.uri, repository.releases, false, repository.snapshots, defaultTransportFactory)
    def apply(repository: Repository, transport: Transport): MavenRepository = apply(repository.id, repository.name, repository.uri, repository.releases, repository.snapshots, false, transport)
    def apply(repository: Repository, transportFactory: TransportFactory): MavenRepository = apply(repository.id, repository.name, repository.uri, repository.releases, repository.snapshots, false, transportFactory)
    
    def defaultArtifactPath(artifact: Artifact): String = {
        return defaultArtifactDir(artifact) + defaultArtifactName(artifact) 
    }

    def defaultArtifactDir(artifact: Artifact): String = {
        return artifact.groupId.replace('.', '/') + '/' + artifact.artifactId + '/' +  artifact.version.toString + '/'
    }

    def defaultArtifactName(artifact: Artifact): String = {
        val res = new StringBuilder
        res.append(artifact.artifactId)
        res.append('-')
        res.append(artifact.version.toString)
        if (artifact.classifier != null) {
            res.append('-')
            res.append(artifact.classifier)
        }
        res.append('.').append(artifact.typ)
        return res.toString
    }

    def artifactName(artifact: Artifact, timestamp: String, buildNumber: String): String = {
        val res = new StringBuilder
        res.append(artifact.artifactId)
        res.append('-')
        res.append(artifact.version.toFinal.toString)
        res.append('-')
        res.append(timestamp)
        res.append('-')
        res.append(buildNumber)
        if (artifact.classifier != null) {
            res.append('-')
            res.append(artifact.classifier)
        }
        res.append('.').append(artifact.typ)
        return res.toString
    }

    def find(artifact: Artifact): ArtifactInstance = find(artifact, repositories)
    
    def find(artifact: Artifact, repositories: Traversable[RepositoryInstance]): ArtifactInstance = {
        val downloaders = startAll(artifact, repositories)

        while (downloaders.size > 0) {
            Actor.receive {
                case inputChannel ! msg => {
                    downloaders.get(inputChannel) match {
                        case Some(actor) => 
                            downloaders -= inputChannel
                            actor.free

                            msg match {
                                case Found(artifactInstance) =>
                                    stopAll(downloaders.values)
                                    return artifactInstance
                                case NotFound(a, r, e) => transport_debug("Not found " + a + " @ " + r, e)
                                case m => throw new RuntimeException("Got strange message from some of DownloadAgents " + m)
                            }
                            
                        
                        case None => null // This is some old actor that didn't finish on time - we should ignore it!
                    }
                }
            }
        }
        null
    }
    
    def ensureInLocal(artifact: Artifact): ArtifactInstance = ensureIn(artifact, repositories, localRepository)
    
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
    def ensureIn(artifact: Artifact, repositories: Traversable[RepositoryInstance], destination: RepositoryInstance): ArtifactInstance = {

        val isSnapshot = !artifact.version.isFinal
        val snapshotInstances = if (artifact.version.isFinal) null else new ListBuffer[ArtifactInstance]
        try {
            val localArtifact = destination.find(artifact) 
            if (isSnapshot) {
                snapshotInstances += localArtifact
            } else {
                return localArtifact
            }
        } catch {
            case notFound: IOException => 
        }
        
        
        val downloaders = startAll(artifact, repositories)

        val foundInstances = new ListBuffer[ArtifactInstance]
        var current: ArtifactInstance = null
        
        def startDownload(artifactInstance: ArtifactInstance) {
            val agent = DownloadActor.getActor
            val future = agent !! Install(artifactInstance, destination)
            val channel = future.inputChannel
            downloaders.put(channel, agent)
            current = artifactInstance
        }
        
        while (downloaders.size > 0) {
            Actor.receive {
                case inputChannel ! msg => {
                    downloaders.get(inputChannel) match {
                        case Some(actor) => {
                            downloaders -= inputChannel
                            actor.free

                            msg match {
                                case Found(artifactInstance) =>
                                    this.synchronized {
                                        if (isSnapshot) {
                                            snapshotInstances += artifactInstance
                                        } else {
                                            if (current == null) {
                                                startDownload(artifactInstance)
                                            } else {
                                                foundInstances += artifactInstance
                                            }
                                        }
                                    }
                                case Installed(resultInstance) =>
                                    stopAll(downloaders.values)
                                    return resultInstance
                                case NotFound(a, r, e) => transport_debug("Not found " + a + " @ " + r, e)
                                case NotInstalled(a, e) =>
                                    this.synchronized {
                                        current = null
                                        if (foundInstances.size > 0) {
                                            current = foundInstances.head
                                            foundInstances.trimStart(1)
                                        }
                                        if (current != null) {
                                            startDownload(current)
                                        }
                                    }
                                case m => throw new RuntimeException("Got strange message from some of DownloadAgents " + m)
                            }
                        }
                        case None => null // This is some old actor that didn't finish on time - we should ignore it!
    
                    }
                }
            }
        }
        if (isSnapshot) {
            val sortedSnapshotInstances = snapshotInstances.sortWith({
                (a: ArtifactInstance, b: ArtifactInstance) => a.lastModified > b.lastModified
            })
            for (artifactInstance <- sortedSnapshotInstances) {
                startDownload(artifactInstance)

                while (downloaders.size > 0) {
                    Actor.receive {
                        case inputChannel ! msg => {
                            downloaders.get(inputChannel) match {
                                case Some(actor) => {
                                    downloaders -= inputChannel
                                    actor.free
        
                                    msg match {
                                        case Found(artifactInstance) => // some odd old message - ignoring it!
                                        case NotFound(a, r, e) => transport_debug("Not found " + a + " @ " + r, e)
                                        case Installed(resultInstance) => // some odd old message - ignoring it!
                                            stopAll(downloaders.values)
                                            return resultInstance
                                        case NotInstalled(a, e) => // some odd old message - ignoring it!
                                        case m => throw new RuntimeException("Got strange message from some of DownloadAgents " + m)
                                    }
                                }
                                case None => null // This is some old actor that didn't finish on time - we should ignore it!
            
                            }
                        }
                    }
                }
                
            }
        }
        throw new FileNotFoundException("Cannot resolve " + artifact + " in " + repositoriesToString(repositories))
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
    def resolveTo(artifact: Artifact, 
            repositories: Traversable[RepositoryInstance], 
            destination: RepositoryInstance,
            resolutionCallback: ResolutionCallback): Unit = {
        localResolveToWithSnapshots(artifact match {
            case d: Dependency => d
            case _ => new SimpleDependency (artifact, null, false)
        }, repositories, new ResolutionContext(destination, resolutionCallback))
    }
    
    protected def localResolveToWithSnapshots(
            dependency: Dependency, 
            repositories: Traversable[RepositoryInstance], 
            context: ResolutionContext): Unit = {
        localResolveTo(dependency, repositories, context)
    }
    
    protected def localResolveTo(
            dependency: Dependency, 
            repositories: Traversable[RepositoryInstance], 
            context: ResolutionContext): Unit = {

        var mainArtifact = if (dependency.typ == null) {
            Artifact(dependency.groupId, dependency.artifactId, dependency.version, "jar", dependency.classifier)
        } else {
            dependency
        }

        context.stack.push(mainArtifact match {
                    case x: Dependency => Artifact(x)
                    case x: Artifact => x
                })
        
        debug("Resolving " + dependency + " from " + repositoriesToString(repositories) + " to " + context.destination)

        var finalArtifact: ArtifactInstance = null
        if (!dependency.isPOM) {
            try {
                finalArtifact = ensureIn(mainArtifact, repositories, context.destination)
                context.alreadyHandled += Artifact(mainArtifact)
            } catch {
                case ioe: FileNotFoundException => 
                if (mainArtifact.version.isFinal) {
                    // It is final, so we will check if snapshot is available
                    mainArtifact = MavenArtifact.toSnapshot(mainArtifact)
                    try {
                        finalArtifact = ensureIn(mainArtifact, repositories, context.destination)
                        context.alreadyHandled += Artifact(mainArtifact)
                    } catch {
                        case ioe: FileNotFoundException => if (!dependency.optional) throw ioe else return
                    }
                } else {
                    throw ioe
                }
            }
        }
        
        
        val pomArtifact = MavenArtifact.toPOM(mainArtifact)
        try {
            val pomArtifactInstance = ensureIn(pomArtifact, repositories, context.destination)
            resolvePOM(pomArtifactInstance, repositories, context)
        } catch {
            case notFound: FileNotFoundException => {
                if (dependency.isPOM && !dependency.optional) {
                    throw notFound
                }
                debug("No POM available " + pomArtifact)
            }
            case t: Throwable => { 
                transport_warning("Failed to load pom", t)
                throw t
            }
        }
        context.stack.pop
        if (!dependency.isPOM) {
            context.resolutionCallback.finished(finalArtifact)
        }
    }
    
    protected def resolvePOM(pomArtifactInstance: ArtifactInstance, 
            repositories: Traversable[RepositoryInstance], 
            context: ResolutionContext): Unit = {
        val pom = POM.load(pomArtifactInstance)
        val pomRepos = pom.repositories
        val reps = if (pomRepos.size == 0) repositories else {
            val r = new LinkedHashSet[RepositoryInstance]
            r ++= (toRepositoryInstances(pomRepos))
            r ++= (repositories)
        }
        if ((pom.parentArtifact != null) && (pom.parentPOM == null)) {
            context.poms.get(pom.parentArtifact) match {
                case Some(parentPOM) => pom.updateParentPOM(parentPOM)
                case None => {
                    localResolveTo(new SimpleDependency(Artifact.asPOM(pom.parentArtifact), null, false), reps, context)
                    context.poms.get(pom.parentArtifact) match {
                        case Some(parentPOM) => pom.updateParentPOM(parentPOM)
                        case None => throw new FileNotFoundException("Couldn't resolve parent pom " + pom.parentArtifact)
                    }
                }
            }
        } 
        
        context.poms.put(Artifact.baseVersionedArtifact(pom.artifact), pom)
        
        for (pomsDep <- pom.dependencies) {
            val artifact = if (pomsDep.typ == null) {
                    Artifact.fallbackToType(pomsDep, "jar")
                } else {
                    Artifact(pomsDep)
                }
            if (context.stack.contains(artifact)) {
                throw new IOException("Got to circular reference: " + pomsDep + " is being resolved. Stack " + artifactsToString(context.stack))
            }
            if (context.resolutionCallback.control(artifact) && !context.alreadyHandled.contains(artifact)) {
                localResolveToWithSnapshots(pomsDep, reps, context)
            }
        }
    }
    
    
    protected def startAll(artifact: Artifact, repositories: Traversable[RepositoryInstance]): Map[InputChannel[_], DownloadActor] = {
        val downloaders = new HashMap[InputChannel[_], DownloadActor]
        for (repository <- repositories) {
            val agent = DownloadActor.getActor
            // downloaders += agent
            val future = agent !! Find(artifact, repository)
            val channel = future.inputChannel
            downloaders.put(channel, agent)
        }
        downloaders
    }
    
    protected def stopAll(downloaders: Traversable[DownloadActor]) = {
        for (a <- downloaders) {
            a.free
        }
    }

    protected def repositoriesToString(t: Traversable[RepositoryInstance]): String = {
        val res = new StringBuilder

        var first = true
        for (r <- t) {
            if (first) first = false else res.append(", ")
            res.append("Repository[").append(r.name).append('(').append(r.id).append("),")
            res.append(r.uri).append(']')
        }
        
        res.toString
    }

    protected def artifactsToString(t: Traversable[Artifact]): String = {
        val res = new StringBuilder

        res.append('[')
        var first = true
        for (r <- t) {
            if (first) first = false else res.append(", ")
            res.append(r.toString)
        }
        res.append(']')
        res.toString
    }

    protected def toRepositoryInstances(t: Traversable[Repository]): Traversable[RepositoryInstance] ={
        val res = new ArrayBuffer[RepositoryInstance]
        for (r <- t) {
            r match {
                case r: RepositoryInstance => res += r
                case r: Repository => res += MavenRepository(r)
            }
        }
        res
    }

    val ALWAYS_ACCEPT = { a: Artifact => true }
    
    def RESOLVE_DEEP(finishedCallback: ArtifactInstance => Unit) = {
        artifactInstance: ArtifactInstance =>
        
        
            finishedCallback(artifactInstance);
    }
}

class MavenRepository(
        id: String, 
        name: String, 
        uri: URI, 
        releases: Boolean, 
        snapshots: Boolean,
        local: Boolean,
        val transport: Transport) extends RepositoryImpl(id, name, uri, releases, snapshots) 
                                 with RepositoryInstance with Log {

    protected def isLocal = local
    protected def installArtifactPath(artifact: Artifact, artifactFullPath: String) = if (local) {
            artifactFullPath
        } else if (artifact.version.isFinal) {
            throw new IOException("Installing to remote repository " + this + " not yet implemented")
        } else {
            throw new IOException("Installing to remote repository " + this + " not yet implemented")
        }
    
    def find(artifact: Artifact): ArtifactInstance = {

        if (artifact.version.isFinal && !releases) {
            throw new FileNotFoundException("Repository doesn't hold releases")
        }
        
        if (!artifact.version.isFinal && !snapshots) {
            throw new FileNotFoundException("Repository doesn't hold snapshots")
        }
        
        val artifactFullPath = MavenRepository.defaultArtifactPath(artifact)
        val artifactDirPath = MavenRepository.defaultArtifactDir(artifact)
        val artifactName = MavenRepository.defaultArtifactName(artifact)
        
        var buildNumber: String = null
        var timestamp: Long = 0
        
        var firstInputStream: Transport.FileStream = null
        var artifactPath: String = null
        // TODO now implement checking if file is correct (local repository), snapshot versions etc...
        
        if (artifact.version.isFinal) {
            artifactPath = artifactFullPath
            firstInputStream = transport.open(artifactPath)
            if (firstInputStream.lastModified != null) {
                timestamp = firstInputStream.lastModified.getTime
            }
//        } else if (local) {
//            artifactPath = artifactFullPath
//            firstInputStream = transport.open(artifactPath)
        } else {
            // Snapshot
            val metadataXMLFileName = artifactDirPath + "maven-metadata.xml"

            try {
                val metadataXMLInputStream = transport.open(metadataXMLFileName)
                val metadataArtifactInstance = new MavenArtifactInstance(this, Artifact(artifact.groupId, artifact.artifactId, artifact.version, "maven-metadata.xml"), 
                        metadataXMLInputStream, metadataXMLFileName)
    
                try {
                    withSHA1check(metadataArtifactInstance) {
                        artifactStream: Transport.FileStream =>
        
                            val xml = XML.load(artifactStream)
                            val timestampString: String = (xml \\ "timestamp" last).text.trim
                            try {
                                val format = new java.text.SimpleDateFormat("yyyyMMdd.HHmmss")
                                timestamp = format.parse(timestampString).getTime()
                            } catch {
                                case ignore: java.text.ParseException => 
                            }
                            
                            buildNumber = (xml \\ "buildNumber" last).text.trim
                            val artifactName = MavenRepository.artifactName(artifact, timestampString, buildNumber)
                            artifactPath = artifactDirPath + artifactName
                            firstInputStream = transport.open(artifactPath)
            
                    }
                } catch {
                    case x: SHA1DoesNotMatchException => {
                            artifactPath = artifactFullPath
                            firstInputStream = transport.open(artifactPath)
                            if (firstInputStream.lastModified != null) {
                                timestamp = firstInputStream.lastModified.getTime
                            }
                        }
                    case x: Exception => throw x
                }
            } catch {
                case x : FileNotFoundException => {
                    // Remote repository, snapshot and no 'maven-metadata.xml' present. Try just snapshot jar itself!

                    artifactPath = artifactFullPath
                    firstInputStream = transport.open(artifactPath)
                    if (firstInputStream.lastModified != null) {
                        timestamp = firstInputStream.lastModified.getTime
                    }
                }
                case x => throw x
            }
        }
        
        
        val res = new MavenArtifactInstance(this, artifact, firstInputStream, artifactPath)
        res.timestamp = timestamp
        res.buildNumber = buildNumber
        res
    }

    def install(artifactInstance: ArtifactInstance): ArtifactInstance = {
        
        val artifact = artifactInstance.artifact
        
        val artifactFullPath = MavenRepository.defaultArtifactPath(artifact)
        val artifactDirPath = MavenRepository.defaultArtifactDir(artifact)
        val artifactName = MavenRepository.defaultArtifactName(artifact)
        
        // TODO now implement checking if file is correct (local repository), snapshot versions etc...
        val artifactPath = installArtifactPath(artifact, artifactFullPath)

        try {
            withSHA1check(artifactInstance) {
                artifactStream: Transport.FileStream =>
                    transport.copy(artifactStream, artifactPath)
    
            } withCalculatedSHA1 {
                sha1: String => 
                    transport.copy(new Transport.FileStreamImpl(new ByteArrayInputStream(sha1.getBytes))
                    {
                        def size = sha1.length
                        def lastModified = null
                    }, artifactPath + ".sha1")
            }
        } catch {
            case x: SHA1DoesNotMatchException => {
                    Transport.ignoreIOError(transport.delete(artifactPath))
                    Transport.ignoreIOError(transport.delete(artifactPath + ".sha1"))
                    throw x
                }
            case x: Exception => throw x
        }
        
        val res = new MavenArtifactInstance(this, artifact, null, artifactPath)
        res
    }
    
    private def checkSha1(m: MavenArtifactInstance): Boolean = {
        true
    }

}

class LocalMavenRepository(
        id: String, name: String, 
        repositoryDir: File, 
        releases: Boolean, 
        snapshots: Boolean)
        
   extends MavenRepository(id, 
           name, 
           repositoryDir.toURI, 
           releases, 
           snapshots, 
           true,
           new FileTransport(repositoryDir)) with Log

class URLRemoteMavenRepository(
        id: String, name: String, 
        repositoryURL: URL, 
        releases: Boolean, 
        snapshots: Boolean)
        
    extends MavenRepository(id, 
            name, 
            repositoryURL.toURI, 
            releases, 
            snapshots, 
            false,
            new URLTransport(repositoryURL)) with Log



trait ResolutionCallback {
    def control(artifact: Artifact): Boolean
    def finished(artifactInstance: ArtifactInstance): Unit
}
            
protected class ResolutionContext(val destination: RepositoryInstance, 
        val resolutionCallback: ResolutionCallback) {
    val poms = new HashMap[Artifact, POM]
    val stack = new Stack[Artifact]
    val alreadyHandled = new HashSet[Artifact]
}
    
protected class SimpleDependency(artifact: Artifact, val scope: String, val optional: Boolean) extends Dependency {
    def groupId = artifact.groupId
    def artifactId = artifact.artifactId
    def version = artifact.version
    def typ = artifact.typ
    def classifier = artifact.classifier
    
    def exclusions: Set[Artifact] = new scala.collection.immutable.Set.EmptySet[Artifact]
}


object withSHA1check extends Log {

    import org.abstracthorizon.extend.repository.util.IOUtils._
        

    def apply(artifactInstance: ArtifactInstance) = {
        new ReadyForProcessing(artifactInstance)
    }
    
    class ReadyForProcessing(artifactInstance: ArtifactInstance) {
        var body: (Transport.FileStream => Unit) = null
        var localSha1: String = null
        
        def apply(body: Transport.FileStream => Unit) = {
            val artifactStream = artifactInstance.stream
            val digestArtifactStream = new DigestStream(artifactStream)
            try {
                body(digestArtifactStream)
            } finally {
                digestArtifactStream.close
            }
            var remoteSha1: String = null
            try {
                artifactInstance match {
                    case m: MavenArtifactInstance => {
                        val sha1Stream = m.sha1Stream
                        try {
                            val sha1 = sha1Stream.split(" ")(0).trim
                            remoteSha1 = sha1
                        } finally {
                            sha1Stream.close
                        }
                    }
                    case _ =>
                }
            } catch {
                case x: FileNotFoundException =>
                case x: IOException => throw x
            }

            localSha1 = digestArtifactStream.sha1Hash
            if (remoteSha1 != null) {
                if (!localSha1.equals(remoteSha1)) {
                    debug("Sha1s do not match " + remoteSha1 + " != " + localSha1 + "; " + artifactInstance.artifact)
                    throw new SHA1DoesNotMatchException("Sha1s do not match " + remoteSha1 + " != " + localSha1 + "; " + artifactInstance.artifact)
                } else {
                    debug("Verified SHA1 digest of " + artifactInstance.artifact)
                }
            } else {
                debug("Artifact doesn't have SHA1 digest; " + artifactInstance.artifact)
            }
            this
        }
        
        def withCalculatedSHA1(body: String => Unit) = {
            body(localSha1)
        }
    }
}

class SHA1DoesNotMatchException(msg: String) extends IOException(msg)
