package org.abstracthorizon.extend.repository

import scala.collection.mutable.ArrayBuffer

import java.io._
import java.net.URI
import java.net.URL
import java.net.HttpURLConnection


trait Repository {
    def id: String
    def name: String
    def uri: URI
    def releases: Boolean
    def snapshots: Boolean
    
    override def toString = "Repository[" + id + "," + name + "," + uri + "," + releases + "," + snapshots + "]"
}


trait RepositoryInstance extends Repository {
    def find(artifact: Artifact): ArtifactInstance
    def install(artifactInstance: ArtifactInstance): ArtifactInstance

    override def toString = "RepositoryInstance[" + id + "," + name + "," + uri + "," + releases + "," + snapshots + "]"
}

class RepositoryImpl(
        val id: String, 
        val name: String, 
        val uri: URI, 
        val releases: Boolean, 
        val snapshots: Boolean) extends Repository {
}

abstract class RepositoryInstanceImpl(
        val id: String, 
        val name: String, 
        val uri: URI, 
        val releases: Boolean, 
        val snapshots: Boolean) extends RepositoryInstance {
}

trait ArtifactInstance {
    def lastModified: Long
    def artifact: Artifact
    def repository: Repository
    
    def stream: Transport.FileStream

    override def toString = "ArtifactInstance[" + artifact + " @ \"" + repository.uri + "\"" + 
                            // "(" + repository + ")" +
                            "]"
}

abstract class AbstractArtifactInstance(
        val repository: Repository, 
        val artifact: Artifact) extends ArtifactInstance {
}

