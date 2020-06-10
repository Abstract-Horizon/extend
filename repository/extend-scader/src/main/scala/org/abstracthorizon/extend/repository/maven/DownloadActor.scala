package org.abstracthorizon.extend.repository.maven

import java.io._

import org.abstracthorizon.extend.repository._

import scala.actors._

import scala.collection.mutable.Set
import scala.collection.mutable.LinkedHashSet

import java.util.concurrent.ThreadFactory

object DownloadActor extends Log {
    import java.util.concurrent.Executors
    
    // TODO make smart executor that maintains steady number of 'working' thread
    // and move those that are 'invalidated' to 'dying' list.
    // Outcome is that number of thread that are doing something sensible (downloading?)
    // is predefined and around the same value, while number of all those that are 'invalidated'
    // can grow over time and those threads not reused directly (unless first thread is below
    // asked value)
    val pool = Executors.newFixedThreadPool(50, new ThreadFactory {
        var num = 1
        def newThread(runnable: Runnable): Thread = {
            val thread = new Thread(runnable)
            thread.setDaemon(true)
            thread.setName("DL " + num)
            num = num + 1
            // thread.start
            thread
        }
    })

    
    
    protected[maven] val freeActors = new LinkedHashSet[DownloadActor]
    protected[maven] val busyActors = new LinkedHashSet[DownloadActor]
    protected[maven] val timingOutActors = new LinkedHashSet[DownloadActor]
    
    def getActor: DownloadActor = {
        var actor: DownloadActor = null
        this.synchronized {
            if (freeActors.size > 0) {
                actor = freeActors.head
                freeActors -= actor
            }
            if (actor == null) {
                actor = new DownloadActor
            }
            actor.allocate()
        }
        actor
    }

}

case class Find(artifact: Artifact, repository: RepositoryInstance) // : ArtifactInstance
case class Install(artifactInstance: ArtifactInstance, repository: RepositoryInstance) // : ArtifactInstance

case class Found(artifactInstance: ArtifactInstance)
case class Installed(artifactInstance: ArtifactInstance)

case class NotFound(artifact: Artifact, repositoryInstance: RepositoryInstance, reason: Throwable)
case class NotInstalled(artifactInstance: ArtifactInstance, reason: Throwable)



class DownloadActor extends Actor with Log {

    class State
    case object Free extends State
    case object Busy extends State
    case object TimingOut extends State
    case object Done extends State
    
    var thread: Thread = null
    
    var owner: Set[DownloadActor] = null
    var state: State = null
    
    override def scheduler = new SchedulerAdapter {
        def execute(block: => Unit) =
            DownloadActor.pool.execute(new Runnable {
            def run() { block }
        })
    } 
    
    protected def changeOwner(newOwner: Set[DownloadActor]) = {
        DownloadActor.synchronized {
            if (owner != null) {
                owner -= this
            }
            this.owner = newOwner
            owner += this
        }
    }
    
    def act = {
        loop {
            react {
                case Find(artifact, sourceRepository) => {
                    thread = Thread.currentThread
                    try {
                        val artifactInstance = sourceRepository.find(artifact)
                        if (Thread.currentThread.isInterrupted) {
                            throw new InterruptedException()
                        }
                        if (artifactInstance != null) {
                            reply(new Found(artifactInstance))
                        } else {
                            reply(new NotFound(artifact, sourceRepository, null))
                        }
                    } catch {
                        case i: InterruptedException => reply(new NotFound(artifact, sourceRepository, i))
                        case e: IOException => reply(new NotFound(artifact, sourceRepository, e))
                        case t: Throwable => reply(new NotFound(artifact, sourceRepository, t))
                    } finally {
                        notifyFinished
                    }
                }
                case Install(artifactInstance, repository) => {
                    thread = Thread.currentThread
                    try {
                        val resultArtifactInstance = repository.install(artifactInstance)
                        if (Thread.currentThread.isInterrupted) {
                            throw new InterruptedException()
                        }
                        if (resultArtifactInstance != null) {
                            reply(new Installed(resultArtifactInstance))
                        } else {
                            reply(new NotInstalled(artifactInstance, null))
                        }
                    } catch {
                        case i: InterruptedException => reply(new NotInstalled(artifactInstance, i))
                        case e: IOException => reply(new NotInstalled(artifactInstance, e))
                        case t: Throwable => reply(new NotInstalled(artifactInstance, t))
                    } finally {
                        notifyFinished
                    }
                }
                case x => {
                    println("ERROR: Received unknown message!" + x)
                    System.exit(1)
                }
            }
        }
    }

    protected def notifyFinished() = {
        this.synchronized {
            if (state == Busy) {
                state = Done
            } else if (state == TimingOut) {
                state = Free
                changeOwner(DownloadActor.freeActors)
            }
        }        
    }
    
    def allocate() = {
        this.synchronized {
            state = Busy
            changeOwner(DownloadActor.busyActors)
        }
    }
    
    def free() = {
        this.synchronized {
            if (state == Busy) {
                thread.interrupt();
                state = TimingOut
                changeOwner(DownloadActor.timingOutActors)
            } else if (state == Done) {
                state = Free
                changeOwner(DownloadActor.freeActors)
            } else if (state == TimingOut) {
                
            } else if (state == Free) {
                
            }
        }
    }
    
    start
}
