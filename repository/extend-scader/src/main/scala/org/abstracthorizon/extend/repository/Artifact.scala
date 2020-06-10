package org.abstracthorizon.extend.repository

trait Artifact {
    def groupId: String
    def artifactId: String
    def version: Version
    def typ: String
    def classifier: String
    
    override def equals(that: Any) = that match {
            case other: Artifact => 
                if (!groupId.equals(other.groupId)) false
                else if (!artifactId.equals(other.artifactId)) false
                else if ((version != other.version) && (version == null || !version.matches(other.version))) false
                else if ((typ != other.typ) && (typ == null || !typ.equals(other.typ))) false
                else if ((classifier != other.classifier) && (classifier == null || !classifier.equals(other.classifier))) false
                else true
            case _ => false
        }
    
    override def hashCode = groupId.hashCode ^ artifactId.hashCode ^
                            (if (version != null) version.hashCode else 0) ^
                            (if (typ != null) typ.hashCode else 0) ^
                            (if (classifier != null) classifier.hashCode else 0)

    override def toString = {
        val res = new StringBuilder
        res.append(groupId)
        res.append(":")
        res.append(artifactId)
        if (version != null) {
            res.append(":")
            res.append(version.toString)
        }
        if (typ != null) {
            if (version == null) { res.append(":") }
            res.append(":")
            res.append(typ)
        }
        if (classifier != null) {
            if (version == null) { res.append(":") }
            if (typ == null) { res.append(":") }
            res.append(":")
            res.append(classifier)
        }
        res.toString
    }

}

object Artifact {
    
    def apply(groupId: String, artifactId: String): Artifact = apply(groupId, artifactId, null.asInstanceOf[Version], null, null)
    def apply(groupId: String, artifactId: String, version: Version): Artifact = apply(groupId, artifactId, version, null, null)
    def apply(groupId: String, artifactId: String, version: Version, typ: String): Artifact = apply(groupId, artifactId, version, typ, null)
    def apply(groupId: String, artifactId: String, version: Version, typ: String, classifier: String): Artifact = {
        val ret = new ArtifactImpl
        ret.groupId = groupId
        ret.artifactId = artifactId
        if (version != null) {
            ret.version = version
        }
        if (typ != null && typ.length > 0) {
            ret.typ = typ
        }
        if (classifier != null && classifier.length > 0) {
            ret.classifier = classifier
        }
        ret
    }
    
    def apply(other: Artifact): Artifact = apply(other.groupId, other.artifactId, other.version, other.typ, other.classifier) 

    def asPOM(other: Artifact): Artifact = apply(other.groupId, other.artifactId, other.version, "pom", null)
    
    def ensureType(other: Artifact, typ: String): Artifact = {
        if (other.getClass == classOf[Artifact]) {
            if (typ.equals(other.typ)) {
                return other
            }
        }
        return apply(other.groupId, other.artifactId, other.version, typ, other.classifier)
    }
    
    def fallbackToType(other: Artifact, typ: String): Artifact = {
        var t = typ
        if (other.getClass == classOf[Artifact]) {
            if (other.typ != null) {
                return other
            }
            t = other.typ
        }
        return apply(other.groupId, other.artifactId, other.version, typ, other.classifier)
    }

    def baseArtifact(artifact: Artifact): Artifact = apply(artifact.groupId, artifact.artifactId)

    def baseVersionedArtifact(artifact: Artifact): Artifact = apply(artifact.groupId, artifact.artifactId, artifact.version)

}


class ArtifactImpl extends Artifact {
    var groupId: String = null
    var artifactId: String = null
    var version: Version = null
    var typ: String = null
    var classifier: String = null
}


