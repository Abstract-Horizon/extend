package org.abstracthorizon.extend.repo;

public class ArtifactImpl implements Artifact {

    private String groupId;
    private String artifactId;
    private Version version;
    private String typ;
    private String classifier;
    
    private ArtifactImpl() {
    }
    
    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getType() {
        return typ;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }
    
    public boolean equals(Object that) {
        if (that instanceof Artifact) {
            Artifact other = (Artifact)that;
            if (!getGroupId().equals(other.getGroupId())) { return false;
            } else if (!getArtifactId().equals(other.getArtifactId())) { return false;
            } else if ((getVersion() != other.getVersion()) && (getVersion() == null || !getVersion().matches(other.getVersion()))) { return false;
            } else if ((getType() != other.getType()) && (typ == null || !getType().equals(other.getType()))) { return false;
            } else if ((getClassifier() != other.getClassifier()) && (getClassifier() == null || !getClassifier().equals(other.getClassifier()))) { return false;
            } else {
                return true;
            }
        } else {
            return super.equals(that);
        }
    }

    public int hashCode() {
        int hashCode = getGroupId().hashCode() ^ getArtifactId().hashCode();
        if (getVersion() != null) { hashCode = hashCode ^ getVersion().hashCode(); }
        if (getType() != null) { hashCode = hashCode ^ getType().hashCode(); }
        if (getClassifier() != null) { hashCode = hashCode ^ getClassifier().hashCode(); }
        return hashCode;
    }
    
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append(getGroupId());
        res.append(":");
        res.append(getArtifactId());
        if (getVersion() != null) {
            res.append(":");
            res.append(getVersion().toString());
        }
        if (getType() != null) {
            if (getVersion() == null) { res.append(":"); }
            res.append(":");
            res.append(getType());
        }
        if (getClassifier() != null) {
            if (getVersion() == null) { res.append(":"); }
            if (getType() == null) { res.append(":"); }
            res.append(":");
            res.append(getClassifier());
        }
        return res.toString();
    }

    
    
    public static Artifact apply(String groupId, String artifactId) {
        return apply(groupId, artifactId, (Version)null, null, null);
    }

    public static Artifact apply(String groupId, String artifactId, Version version) {
        return apply(groupId, artifactId, version, null, null);
    }

    public static Artifact apply(String groupId, String artifactId, Version version, String typ) {
        return apply(groupId, artifactId, version, typ, null);
    }

    public static Artifact apply(String groupId, String artifactId, Version version, String typ, String classifier) {
        ArtifactImpl ret = new ArtifactImpl();
        ret.groupId = groupId;
        ret.artifactId = artifactId;
        if (version != null) {
            ret.version = version;
        }
        if (typ != null && typ.length() > 0) {
            ret.typ = typ;
        }
        if (classifier != null && classifier.length() > 0) {
            ret.classifier = classifier;
        }
        return ret;
    }
    
    public static Artifact apply(Artifact other) {
        return apply(other.getGroupId(), other.getArtifactId(), other.getVersion(), other.getType(), other.getClassifier());
    }

    public static Artifact asPOM(Artifact other) { 
        return apply(other.getGroupId(), other.getArtifactId(), other.getVersion(), "pom", null);
    }
    
    public static Artifact ensureType(Artifact other, String typ) {
        if (typ.equals(other.getType())) {
            return other;
        }
        return apply(other.getGroupId(), other.getArtifactId(), other.getVersion(), typ, other.getClassifier());
    }
    
    public static Artifact fallbackToType(Artifact other, String typ) {
        if (other.getType() != null) {
            return other;
        }
        return apply(other.getGroupId(), other.getArtifactId(), other.getVersion(), typ, other.getClassifier());
    }

    public static Artifact baseArtifact(Artifact artifact) {
        return apply(artifact.getGroupId(), artifact.getArtifactId());
    }

    public static Artifact baseVersionedArtifact(Artifact artifact) {
        return apply(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }
    
    
}
