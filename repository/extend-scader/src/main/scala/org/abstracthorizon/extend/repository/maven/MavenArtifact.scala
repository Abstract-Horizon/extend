package org.abstracthorizon.extend.repository.maven

import org.abstracthorizon.extend.repository._

object MavenArtifact {
    def apply(s: String): Artifact = {
        val list = s.split(":")
        if (list.length == 2) {
            return Artifact.apply(list(0), list(1))
        } else if (list.length == 3) {
            return Artifact.apply(list(0), list(1), MavenVersion(list(2)))
        } else if (list.length == 4) {
            return Artifact.apply(list(0), list(1), MavenVersion(list(2)), list(3))
        } else if (list.length == 5) {
            return Artifact.apply(list(0), list(1), MavenVersion(list(2)), list(3), list(4))
        } else {
            return null
        }
    }

    def toPOM(artifact: Artifact): Artifact = if ("pom".equals(artifact.typ)) artifact else Artifact(artifact.groupId, artifact.artifactId, artifact.version, "pom", artifact.classifier)

    def toFinal(artifact: Artifact): Artifact = if (artifact.version.isFinal) artifact else Artifact(artifact.groupId, artifact.artifactId, artifact.version.toFinal, artifact.typ, artifact.classifier)

    def toSnapshot(artifact: Artifact): Artifact = if (!artifact.version.isFinal) artifact else Artifact(artifact.groupId, artifact.artifactId, MavenVersion.toSnapshot(artifact.version), artifact.typ, artifact.classifier)
}