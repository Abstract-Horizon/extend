package org.abstracthorizon.extend.repository.maven.pom;

import java.net.URL;

public class RepositoryDefinition {

    private String id;
    private URL url;
    private boolean releasesEnabled;
    private boolean snapshotsEnabled;
    
    public RepositoryDefinition() {
    }

    public RepositoryDefinition(String id, URL url, boolean releasesEnabled, boolean snapshotEnabled) {
        this.id = id;
        this.url = url;
        this.releasesEnabled = releasesEnabled;
        this.snapshotsEnabled = snapshotEnabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public boolean isReleasesEnabled() {
        return releasesEnabled;
    }

    public void setReleasesEnabled(boolean releasesEnabled) {
        this.releasesEnabled = releasesEnabled;
    }

    public boolean isSnapshotsEnabled() {
        return snapshotsEnabled;
    }

    public void setSnapshotsEnabled(boolean snapshotsEnabled) {
        this.snapshotsEnabled = snapshotsEnabled;
    }
    
    public String toString() {
        return "Repository[" + id + "," + url.toString() + ",releases=" + releasesEnabled + ",snapshots=" + snapshotsEnabled + "]";
    }
}
