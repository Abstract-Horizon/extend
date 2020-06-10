package org.abstracthorizon.extend.repo.maven;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactImpl;

public class MavenArtifact {

    public static Artifact apply(String s) {
        String[] list = s.split(":");
        if (list.length == 2) {
            return ArtifactImpl.apply(list[0], list[1]);
        } else if (list.length == 3) {
            return ArtifactImpl.apply(list[0], list[1], MavenVersion.apply(list[2]));
        } else if (list.length == 4) {
            return ArtifactImpl.apply(list[0], list[1], MavenVersion.apply(list[2]), list[3]);
        } else if (list.length == 5) {
            return ArtifactImpl.apply(list[0], list[1], MavenVersion.apply(list[2]), list[3], list[4]);
        } else {
            return null;
        }
    }
 
    public static Artifact toPOM(Artifact artifact) {
        if ("pom".equals(artifact.getType())) {
            return artifact;
        } else {
            return ArtifactImpl.apply(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "pom", artifact.getClassifier());
        }
    }
    
    public static Artifact toFinal(Artifact artifact) {
        if (artifact.getVersion().isFinal()) {
            return artifact;
        } else { 
            return ArtifactImpl.apply(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion().toFinal(), artifact.getType(), artifact.getClassifier());
        }
    }
    
    public static Artifact toSnapshot(Artifact artifact) {
        if (!artifact.getVersion().isFinal()) {
            return artifact;
        } else { 
            return ArtifactImpl.apply(artifact.getGroupId(), artifact.getArtifactId(), MavenVersion.toSnapshot(artifact.getVersion()), artifact.getType(), artifact.getClassifier());
        }
        
    }
}

/*


    def toSnapshot(artifact: Artifact): Artifact = if (!artifact.version.isFinal) artifact else Artifact(artifact.groupId, artifact.artifactId, MavenVersion.toSnapshot(artifact.version), artifact.typ, artifact.classifier)
*/