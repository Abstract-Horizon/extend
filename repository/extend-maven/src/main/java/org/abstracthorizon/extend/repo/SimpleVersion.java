package org.abstracthorizon.extend.repo;

public class SimpleVersion implements Version {
    
    private String versionString;
    private boolean isFinal;
    
    public SimpleVersion(String versionString) {
        this.versionString = versionString;
    }
    
    protected String getVersionString() {
        return versionString;
    }
    
    public boolean matches(Version version) {
        if (version instanceof SimpleVersion) {
            return versionString.equals(((SimpleVersion)version).versionString);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return versionString.hashCode();
    }
    
    public String toString() {
        return versionString;
    }
    
    public boolean isFinal() {
        return isFinal;
    }

    public Version toFinal() {
        if (isFinal) {
            return this;
        } else {
            SimpleVersion ver = new SimpleVersion(versionString);
            ver.isFinal = true;
            return ver;
        }
    }
}
