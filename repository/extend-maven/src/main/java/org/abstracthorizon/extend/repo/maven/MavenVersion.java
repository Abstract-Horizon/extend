package org.abstracthorizon.extend.repo.maven;

import org.abstracthorizon.extend.repo.SimpleVersion;
import org.abstracthorizon.extend.repo.Version;

public class MavenVersion extends SimpleVersion {

    public MavenVersion(String versionString) {
        super(versionString);
    }
    
    public static MavenVersion apply(String versionString) {
        if (versionString != null && versionString.trim().length() > 0) {
            return new MavenVersion(versionString);
        } else {
            return null;
        }
    }
    
    public static MavenVersion toSnapshot(Version version) {
        if (version.isFinal()) {
            return new MavenVersion(version.toString() + "-SNAPSHOT");
        } else {
            if (version instanceof MavenVersion) {
                return (MavenVersion) version;
            } else {
                return new MavenVersion(version.toString());
            }
        }
    }
    
    @Override
    public boolean isFinal() {
        return !getVersionString().endsWith("-SNAPSHOT");
    }
    
    @Override
    public Version toFinal() {
        if (isFinal()) {
            return this;
        } else {
            String versionString = getVersionString();
            return new MavenVersion(versionString.substring(0, versionString.length() - 9));
        }
    }
}
