package org.abstracthorizon.extend.repository.maven

import java.io._
import java.net.URI
import org.abstracthorizon.extend.repository._
import scala.collection.mutable.HashMap

class InMemoryRepository extends Transport {


    val files = new HashMap[String, Array[Byte]]
    val lastModifiedTimestamps = new HashMap[String, java.util.Date]
    
    def clear =  {
        files.clear
    }
    
    def open(path: String): Transport.FileStream = {
        files.get(path) match {
            case Some(array) => new Transport.FileStreamImpl(new ByteArrayInputStream(array)) {
                def size = array.length
                def lastModified = lastModifiedTimestamps.getOrElse(path, null)
            }
            case None => throw new FileNotFoundException(path)
        }
    }
    
    def copy(inputStream: Transport.FileStream, path: String): Unit = {
        
        val bodyStream = new ByteArrayOutputStream
        val buffer = new Array[Byte](10240)
        var r = inputStream.read(buffer)
        while (r > 0) {
            bodyStream.write(buffer, 0, r)
            r = inputStream.read(buffer)
        }
        inputStream.close
        files.put(path, bodyStream.toByteArray)
    }

    def delete(path: String) = new UnsupportedOperationException
}

class ByteArrayArtifactInstance(buffer: Array[Byte], artifact: Artifact) extends AbstractArtifactInstance(null, artifact) {
    var lastModified: Long = -1
    def stream: Transport.FileStream = new Transport.FileStreamImpl(new ByteArrayInputStream(buffer)) {
                def size = buffer.length
                def lastModified = null
            }
}

class ReflectiveRepositoryInstance extends RepositoryInstanceImpl("destId", "Destination Id", new URI("destination://here"), true, true) {
        def find(artifact: Artifact): ArtifactInstance = throw new FileNotFoundException(artifact.toString)
        def install(artifactInstance: ArtifactInstance): ArtifactInstance = {
            new AbstractArtifactInstance(artifactInstance.repository /* This is to be able to read which repository we got it from! */, 
                                         artifactInstance.artifact) {
                var lastModified: Long = -1
                def stream: Transport.FileStream =  new Transport.FileStreamImpl(new ByteArrayInputStream(artifactInstance.artifact.toString.getBytes)) {
                    def size = artifactInstance.artifact.toString.length
                    def lastModified = null
                }
            }
        }
    }