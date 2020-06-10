package org.abstracthorizon.extend.repo;


public abstract class AbstractArtifactInstance implements ArtifactInstance {

    private Repository repository;
    private Artifact artifact;
        
    public AbstractArtifactInstance(Repository repository, Artifact artifact) {
        this.repository = repository;
        this.artifact = artifact;
    }
    
    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    protected void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    protected void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
    
    @Override 
    public String toString() {
        return "ArtifactInstance[" + getArtifact() + " @ \"" + getRepository().getURI() + "\"" + "]";
        // "(" + repository + ")" +
    }

}
