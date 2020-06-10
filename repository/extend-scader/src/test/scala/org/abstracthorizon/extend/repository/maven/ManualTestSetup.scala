package org.abstracthorizon.extend.repository.maven

import scala.actors._

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import java.util.Random
import java.net.URI
import java.io.File

import org.abstracthorizon.extend.repository._

object ManualTestSetup extends Log {
    
    
    def main(args: Array[String]) = {
        
        val central = MavenRepository("central", "Central", new URI("http://repo1.maven.org/maven2"), true, true, false)
        val ahRelease = MavenRepository("ah-release", "AH Release Repo", new URI("http://repository.abstracthorizon.org/maven2/abstracthorizon/"), true, false, false)
        val ahSnapshot = MavenRepository("ah-snapshot", "AH Snapshot Repo", new URI("http://repository.abstracthorizon.org/maven2/abstracthorizon.snapshot/"), false, true, false)

        val repositoryDir = File.createTempFile("test-local-repo", ".m2.dir")
        repositoryDir.delete
        repositoryDir.mkdirs
        
        info("Test local repository at " + repositoryDir.getAbsolutePath)
        
        val localTestRepository = new LocalMavenRepository("local", "Local repository", repositoryDir, true, true)
        
        MavenRepository.repositories += central
        MavenRepository.repositories += ahRelease
        MavenRepository.repositories += ahSnapshot
    
        // val version = MavenVersion("1.0")
        // val groupId = "org.ah.tzatziki"
        // val artifactName = "tzatziki-scala"
        // val version = MavenVersion("1.2-SNAPSHOT")
        val groupId = "org.abstracthorizon.extend"
        val artifactName = "extend-core"
        val version = MavenVersion("1.2-SNAPSHOT")
        val loaded = new HashSet[Artifact]
        MavenRepository.resolveTo(
            Artifact(groupId, artifactName, version, "jar"), 
            MavenRepository.repositories, 
            localTestRepository, 
            new ResolutionCallback{ 
                def control(a: Artifact) = {
                    val ta = Artifact(a)
                    val res = loaded.contains(ta)
                    info("         " + ta + " => " + res + " in " + loaded)
                    !res 
                }
                
                def finished(artifactInstance: ArtifactInstance) = {
                
                    info(" *** Got " + artifactInstance)
                    loaded.add(Artifact(artifactInstance.artifact))
                    info("         " + loaded);
                }
        })
     
        info(" ====================== Finished ======================= ")
        System.exit(0)
        
    }


}
