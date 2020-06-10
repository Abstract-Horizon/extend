package org.abstracthorizon.extend.repo.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactImpl;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.Repository;
import org.abstracthorizon.extend.repo.RepositoryImpl;
import org.abstracthorizon.extend.repo.Version;
import org.abstracthorizon.extend.repository.maven.SubstitutionTraverser;
import org.abstracthorizon.extend.repository.maven.XMLProcessor;
import org.abstracthorizon.extend.repository.maven.pom.POM;

public class MavenPOM {

    // TODO cache loaded poms for the duration of resolve!
    
    public static MavenPOM load(Artifact pomArtifact, InputStream inputStream) throws IOException {
        MavenPOM pom = new MavenPOM();
        pom.read(pomArtifact, inputStream);
        return pom;
    }

    public static MavenPOM load(ArtifactInstance artifactInstance) throws IOException {
        InputStream inputStream = artifactInstance.getStream(); 
        try {
            return load(artifactInstance.getArtifact(), inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private Artifact parentArtifactInt;
    private Artifact artifactInt;
    private String nameInt = null;
    private String descriptionInt = null;
    
    protected void updateParentArtifact(Artifact parentArtifact) { this.parentArtifactInt = parentArtifact; }
    
    MavenPOM cachedParentPOM = null;

    MavenPOM getParentPOM() {
        if (cachedParentPOM != null) {
            return cachedParentPOM;
        }
        return cachedParentPOM;
    }
    
    private Map<Artifact, Dependency> deps = new LinkedHashMap<Artifact, Dependency>();
    private Map<Artifact, Dependency> templateDeps = new LinkedHashMap<Artifact, Dependency>();
    private Map<String, Repository> repositoriesInt = new LinkedHashMap<String, Repository>();

    private boolean parsedDependencies = false;
    private boolean parsedTemplateDependencies = false;
    private boolean parsedRepositories = false;
    
    protected Map<String, String> props = new HashMap<String, String>();
    
    protected POM pom = null;
    
    public void read(Artifact pomArtifact, InputStream inputStream) throws IOException {
        
        pom = new POM();

        try {
            XMLProcessor processor = new XMLProcessor(inputStream);
            processor.setStartObject(pom);
            processor.process();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }

        parseProperties(pom);

        if (pom.getParent() != null) {
            Version parentVersion = new MavenVersion(pom.getParent().getVersion());
            parentArtifactInt = ArtifactImpl.apply(pom.getParent().getGroupId(), 
                    pom.getParent().getArtifactId(), 
                    parentVersion);
        }
            
        
        nameInt = pom.getName();
        descriptionInt = pom.getDescription();

        String groupId = pom.getGroupId();
        String artifactId = pom.getArtifactId();
        String versionString = pom.getVersion();
        String packaging = pom.getPackaging();
        
        
        if (groupId == null) {
            groupId = parentArtifactInt.getGroupId();
        }

        Version version = null;
        if (versionString != null) {
            version = MavenVersion.apply(versionString);
        } else {
            if (getParentPOM() != null) {
                version = getParentPOM().getArtifact().getVersion();
            } else {
                version = null;
            }
        }
        
        artifactInt = ArtifactImpl.apply(groupId, artifactId, version, packaging);
        
        

        HashMap<String, String> properties = new HashMap<String, String>();
        collectProperties(properties, pom);
        if (pom.getVersion() != null) {
            properties.put("pom.version", pom.getVersion());
            properties.put("project.version", pom.getVersion());
        } else {
            Version ver = pomArtifact.getVersion();
            String verStr = "";
            if (ver != null) {
                verStr = ver.toString();
            }
            properties.put("pom.version", verStr);
            properties.put("project.version", verStr);
        }
        if (pom.getGroupId() != null) {
            properties.put("pom.groupId", pom.getGroupId());
            properties.put("project.groupId", pom.getGroupId());
        } else {
            properties.put("pom.groupId", pomArtifact.getGroupId());
            properties.put("project.groupId", pomArtifact.getGroupId());
        }
        if (pom.getArtifactId() != null) {
            properties.put("pom.artifactId", pom.getArtifactId());
            properties.put("project.artifactId", pom.getArtifactId());
        } else {
            properties.put("pom.artifactId", pomArtifact.getArtifactId());
            properties.put("project.artifactId", pomArtifact.getArtifactId());
        }
        SubstitutionTraverser.substitute(pom, properties);

    }

    protected void collectProperties(Map<String, String> properties, POM pom) {
        Map<String, String> p = pom.getProperties();
        if (p != null) {
            for (Map.Entry<String, String> entry : p.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        POM parent = pom.getParentPOM();
        if (parent != null) {
            collectProperties(properties, parent);
        }
    }

    protected void updateParentPOM(MavenPOM parentPOM) {
        cachedParentPOM = parentPOM;
        if (artifactInt.getVersion() == null) {
            artifactInt = ArtifactImpl.apply(artifactInt.getGroupId(), 
                    artifactInt.getArtifactId(), 
                    parentPOM.artifactInt.getVersion(), artifactInt.getType());
        }
    }
    
    protected void parseProperties(POM pom) {
        if (pom.getProperties() != null) {
            for (Map.Entry<String, String> entry : pom.getProperties().entrySet()) {
                this.props.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    protected void parseRepositories() {
        if (pom.getRepositories() != null) {
            for (org.abstracthorizon.extend.repository.maven.pom.Repository repository : pom.getRepositories().getRepositories()) {
                String id = repository.getId();
                String name = repository.getName();
                String uriText = repository.getUrl();
                boolean releases = "true".equalsIgnoreCase(repository.getReleases().getEnabled());
                boolean snapshots = "true".equalsIgnoreCase(repository.getSnapshots().getEnabled());
                URI uri = null;
                if (uriText != null) { 
                    try {
                        uri = new URI(uriText);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                };
                RepositoryImpl repositoryImpl = new RepositoryImpl(id, name, uri, releases, snapshots);
                repositoriesInt.put(id, repositoryImpl);
            }
        }
        parsedRepositories = true;
        checkRelease();
    }
    
    protected void parseDependencies() {
        if (pom.getDependencies() != null) {
            for (org.abstracthorizon.extend.repository.maven.pom.Dependency dependency : pom.getDependencies().getDependencies()) {
                Dependency depImpl = parseDependency(dependency, false);
                        
                deps.put(ArtifactImpl.baseArtifact(depImpl), depImpl);
            }
        }
        
        parsedDependencies = true;
        checkRelease();
    }
    
    protected void parseTemplateDependencies() {
        if (pom.getDependencyManagement() != null && pom.getDependencyManagement().getDependencies() != null) {
            for (org.abstracthorizon.extend.repository.maven.pom.Dependency dependency : pom.getDependencyManagement().getDependencies().getDependencies()) {
                Dependency depImpl = parseDependency(dependency, true);
                        
                templateDeps.put(ArtifactImpl.baseArtifact(depImpl), depImpl);
            }
        }

        parsedTemplateDependencies = true;
        checkRelease();
    }
    
    protected Dependency parseDependency(org.abstracthorizon.extend.repository.maven.pom.Dependency dependency, boolean template) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        
        String version = dependency.getVersion();
        String typ = dependency.getType();
        String classifier = dependency.getClassifier();
        String scope = dependency.getScope();

        InternalDependency internalDependency = new InternalDependency(template);
        internalDependency.update(groupId, artifactId, version, typ, classifier);
        internalDependency.updateScope(scope);
        
        String op = dependency.getOptional();
        if (op != null) {
            internalDependency.updateOptional("true".equalsIgnoreCase(op));
        }
        
        return internalDependency;
    }
    
    protected void checkRelease() {
        if (parsedDependencies && parsedTemplateDependencies && parsedRepositories) {
            pom = null;
        }
    }
        

    public class InternalDependency implements Dependency, Artifact {

        String scopeInt;
        boolean optionalInt = false;
        boolean optionalIntDefined = false;
        boolean template;
        String groupId;
        String artifactId;
        Version version;
        String typ;
        String classifier;

        Set<Artifact> exclusionsThis = new LinkedHashSet<Artifact>();

        public InternalDependency(boolean template) {
            this.template = template;
        }
        
        public String getGroupId() {
            return groupId;
        }
        
        public String getArtifactId() {
            return artifactId;
        }

        public Set<Artifact> getExclusions() {
            Set<Artifact> result = new LinkedHashSet<Artifact>();
            Dependency dependency = findTemplateDependency(getArtifact(), template);
            if (dependency != null) {
                result.addAll(dependency.getExclusions());
            }
            result.addAll(exclusionsThis);
            return result;
        }
        
        protected void addExclusion(Artifact artifact) {
            exclusionsThis.add(artifact);
        }
        
        protected void updateScope(String scope) {
            this.scopeInt = scope;
        }

        protected void updateOptional(boolean optional) {
            this.optionalInt = optional;
            this.optionalIntDefined = true;
        }

        private Version v = null;
        private String t = null;
        private String c = null;
        
        public void update(String groupId, String artifactId, String version, String typ, String classifier) { 
            update(groupId, artifactId, MavenVersion.apply(version), typ, classifier);
        }
            
        public void update(String groupId, String artifactId, Version version, String typ, String classifier) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.v = version;
            this.t = typ;
            this.c = classifier;
        }
        
        public void update(Artifact artifact) {
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
        }
        
        public Version getVersion() {
            if (v != null) return v;

            Dependency dependency = findTemplateDependency(this, template);
            if (dependency != null) {
                return dependency.getVersion();
            }
            throw new POMException("Incomplete POM - missing version for " + this.toStringThis() + " in " + MavenPOM.this.artifactInt.toString());
        }
        
        public String getType() {
            if (t != null) return t;

            Dependency dependency = findTemplateDependency(this, template);
            if (dependency != null) {
                return dependency.getType();
            }
            
            return null;
        }

        public String getClassifier() {
            if (c != null) return c;

            Dependency dependency = findTemplateDependency(this, template);
            if (dependency != null) {
                return dependency.getClassifier();
            }

            return null;
        }

        public String getScope() {
            if (scopeInt != null) return scopeInt;

            Dependency dependency = findTemplateDependency(this, template);
            if (dependency != null) {
                return dependency.getScope();
            }

            return null;
        }

        public boolean isOptional() {
            if (optionalIntDefined) return optionalInt;

            Dependency dependency = findTemplateDependency(this, template);
            if (dependency != null) {
                return dependency.isOptional();
            }

            return false;
        }
        
        public String toStringThis() {
            StringBuilder res = new StringBuilder();
            res.append("POMDependencyArtifact[");
            res.append(groupId);
            res.append(":");
            res.append(artifactId);
            if (v != null) {
                res.append(":");
                res.append(v);
            }
            if (t != null) {
                if (v == null) { res.append(":"); }
                res.append(":");
                res.append(t);
            }
            if (getClassifier() != null) {
                if (v == null) { res.append(":"); }
                if (t == null) { res.append(":"); }
                res.append(":");
                res.append(c);
            }
            res.append("]");
            return res.toString();
        }
        

        public boolean isPOM() {
            return "pom".equals(getType());
        }
        
        public boolean equals(Object that) {
            if (that instanceof Artifact) {
                Artifact other = (Artifact)that;
                if (!groupId.equals(other.getGroupId())) return false;
                else if (!artifactId.equals(other.getArtifactId())) return false;
                else if ((version != other.getVersion()) && (version == null || !version.matches(other.getVersion()))) return false;
                else if ((typ != other.getType()) && (typ == null || !typ.equals(other.getType()))) return false;
                else if ((classifier != other.getClassifier()) && (classifier == null || !classifier.equals(other.getClassifier()))) return false;
                else return true;
            } else {
                return false;
            }
        }
        
        public int hashCode() {
            int hashCode = groupId.hashCode() ^ artifactId.hashCode();
            if (version != null) { hashCode = hashCode ^ version.hashCode(); }
            if (typ != null) { hashCode = hashCode ^ typ.hashCode(); }
            if (classifier != null) { hashCode = hashCode ^  classifier.hashCode(); }
            return hashCode;
        }
        
        public String toString() {
            StringBuilder res = new StringBuilder();
            res.append("Dependency[");
            res.append(toStringThis());
            res.append(", scope=").append(getScope());
            res.append(", optional=").append(isOptional());
            res.append("]");
            return res.toString();
        }
    }
        
    
    /* Public methods */
    
    public Artifact getParentArtifact() {
        return parentArtifactInt;
    }

    public Artifact getArtifact() {
        return artifactInt;
    }
    
    public String getName() {
        return nameInt;
    }
    
    public String getDescription() {
        return descriptionInt;
    }
    
    public Collection<Dependency> getDependencies() {
        if (!parsedDependencies) {
            parseDependencies();
        }
        return deps.values();
    }
    
    public Collection<Repository> getRepositories() {
        if (!parsedRepositories) {
            parseRepositories();
        }
        return repositoriesInt.values();
    }
    
    public Collection<Dependency> templateDependencies() {
        if (!parsedTemplateDependencies) {
            parseTemplateDependencies();
        }
        return templateDeps.values();
    }
    
    public Dependency findTemplateDependency(Artifact artifact, boolean fromTemplate) {
        if (!parsedTemplateDependencies) {
            parseTemplateDependencies();
        }
        Dependency dependency = null;
        if (!fromTemplate) {
            dependency = templateDeps.get(ArtifactImpl.baseArtifact(artifact));
        }
        // var dependency = if (!fromTemplate) templateDeps.getOrElse(Artifact.baseArtifact(artifact), null) else null
        if (dependency == null) {
            if (getParentPOM() != null) {
                dependency = getParentPOM().findTemplateDependency(artifact, false);
            }
        }
        return dependency;
    }
    
    public Dependency addDependency(Artifact artifact) { 
        InternalDependency dependency = new InternalDependency(false);
        dependency.update(artifact);
        deps.put(ArtifactImpl.baseArtifact(dependency), dependency);
        return dependency;
    }

    public Dependency addTemplateDependency(Artifact artifact) { 
        InternalDependency dependency = new InternalDependency(true);
        dependency.update(artifact);
        templateDeps.put(ArtifactImpl.baseArtifact(dependency), dependency);
        return dependency;
    }
    
}
