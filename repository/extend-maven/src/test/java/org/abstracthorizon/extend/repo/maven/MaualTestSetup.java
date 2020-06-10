package org.abstracthorizon.extend.repo.maven;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.abstracthorizon.extend.repo.Artifact;
import org.abstracthorizon.extend.repo.ArtifactImpl;
import org.abstracthorizon.extend.repo.ArtifactInstance;
import org.abstracthorizon.extend.repo.Version;

public class MaualTestSetup {

    public static void info(String msg) {
        System.out.println(msg);
    }
    
    public static void main(String[] args) throws Exception {
        MavenRepositoryFactory factory = new MavenRepositoryFactory();
        MavenRepository central = factory.apply("central", "Central", new URI("http://repo1.maven.org/maven2"), true, true, false);
        MavenRepository ahRelease = factory.apply("ah-release", "AH Release Repo", new URI("http://repository.abstracthorizon.org/maven2/abstracthorizon/"), true, false, false);
        MavenRepository ahSnapshot = factory.apply("ah-snapshot", "AH Snapshot Repo", new URI("http://repository.abstracthorizon.org/maven2/abstracthorizon.snapshot/"), false, true, false);

        File repositoryDir = File.createTempFile("test-local-repo", ".m2.dir");
        repositoryDir.delete();
        repositoryDir.mkdirs();
        
        info("Test local repository at " + repositoryDir.getAbsolutePath());
        
        MavenRepository localTestRepository = factory.localRepository("local", "Local repository", repositoryDir, true, true);
        
        factory.repositories.add(central);
        factory.repositories.add(ahRelease);
        factory.repositories.add(ahSnapshot);
    
        // val version = MavenVersion("1.0")
        // val groupId = "org.ah.tzatziki"
        // val artifactName = "tzatziki-scala"
        // val version = MavenVersion("1.2-SNAPSHOT")
        String groupId = "org.abstracthorizon.extend";
        String artifactName = "extend-core";
        Version version = MavenVersion.apply("1.2-SNAPSHOT");
        final Set<Artifact> loaded = new HashSet<Artifact>();
        factory.resolveTo(
            ArtifactImpl.apply(groupId, artifactName, version, "jar"), 
            factory.repositories, 
            localTestRepository, 
            new MavenRepositoryFactory.ResolutionCallback() { 
                public boolean control(Artifact a) {
                    Artifact ta = ArtifactImpl.apply(a);
                    boolean res = loaded.contains(ta);
                    info("         " + ta + " => " + res + " in " + loaded);
                    return !res; 
                }
                
                public void finished(ArtifactInstance artifactInstance) {
                
                    info(" *** Got " + artifactInstance);
                    loaded.add(ArtifactImpl.apply(artifactInstance.getArtifact()));
                    info("         " + loaded);
                }
        });
     
        System.out.println(" ====================== Finished ======================= ");
        System.exit(0);

    }
    
}
