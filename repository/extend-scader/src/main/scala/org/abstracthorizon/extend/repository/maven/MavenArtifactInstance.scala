package org.abstracthorizon.extend.repository.maven

import java.io._
import java.text.SimpleDateFormat
import java.text.ParseException

import org.abstracthorizon.extend.repository._

class MavenArtifactInstance(repository: MavenRepository, artifact: Artifact, var initialInputStream: Transport.FileStream, artifactPath: String) extends AbstractArtifactInstance(repository, artifact) {
    
    var timestamp: Long = 0
    var buildNumber: String = null

    def lastModified: Long = timestamp

    def stream: Transport.FileStream = if (initialInputStream != null) {
        val res = initialInputStream
        initialInputStream = null
        res
    } else {
        repository.transport.open(artifactPath)
    }

    def sha1Stream: InputStream = repository.transport.open(artifactPath + ".sha1")

}
