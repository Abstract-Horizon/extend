package org.abstracthorizon.extend.repository.maven.pom;

public class ReleasesAndSnapshots {

    protected boolean enabled;
    
    public ReleasesAndSnapshots() {
    }

    public ReleasesAndSnapshots(boolean defaultEnabled) {
        this.enabled = defaultEnabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = "true".equalsIgnoreCase(enabled);
    }

    public String getEnabled() {
        if (enabled) {
            return "true";
        } else {
            return "false";
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
    
    public String toString(int indent) {
        StringBuffer res = new StringBuffer();
        String tag = null;
        if (this instanceof Releases) {
            tag = "releases";
        } else {
            tag = "snapshots";
        }
        
        ToStringHelper.openTag(tag, res, indent);
        ToStringHelper.valueTag("enabled", getEnabled(), res, indent + 2);
        ToStringHelper.closeTag(tag, res, indent);
        return res.toString();
    }
    
    public String toString() {
        return toString(0);
    }

}
