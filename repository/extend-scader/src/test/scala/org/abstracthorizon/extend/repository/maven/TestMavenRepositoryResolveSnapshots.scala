package org.abstracthorizon.extend.repository.maven

import org.junit.Assert._
import org.ah.tzatziki.JUnitFeature
import org.ah.tzatziki.Table

import scala.xml.XML 
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer

import java.io._
import java.net.URI
import java.util.Random


import org.abstracthorizon.extend.repository.Repository
import org.abstracthorizon.extend.repository.RepositoryImpl
import org.abstracthorizon.extend.repository.RepositoryInstance

import org.abstracthorizon.extend.repository.Artifact
import org.abstracthorizon.extend.repository.ArtifactInstance

import org.abstracthorizon.extend.repository.Transport

class TestMavenRepositoryResolveSnapshots extends JUnitFeature {

var feature = 
"""
Feature: Maven Repository Resolve Snapshots

  Background: something that is executed always
    Given cleared all repositories
      And I have remote repository "remote", "Test Remote Repo" with uri "memory://remote/"
      And I use remote repository

  Scenario: Resolve simple SNAPSHOT artifact
    Given I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with no dependencies
      And I have artifact "org.group:artifact:1.0-SNAPSHOT:jar"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back "org.group:artifact:1.0-SNAPSHOT:jar"

  Scenario: Fallback to SNAPSHOT if final asked 
    Given I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with no dependencies
      And I have artifact "org.group:artifact:1.0-SNAPSHOT:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact:1.0-SNAPSHOT:jar"

  Scenario: Resolve deep for POM with shared transitive dependencies and SNAPSHOT fallbacks
    Given I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with dependencies
       |dependency|
       |org.group:artifact2:2:jar|
       |org.group:artifact3:3:jar|
      And I have "org.group:artifact2:2-SNAPSHOT:jar" pom with dependencies
       |dependency|
       |org.group:artifact3:3:jar|
       |org.group:artifact4:4:jar|
      And I have artifact "org.group:artifact:1.0-SNAPSHOT:jar"
      And I have artifact "org.group:artifact2:2-SNAPSHOT:jar"
      And I have artifact "org.group:artifact3:3:jar"
      And I have artifact "org.group:artifact4:4:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact3:3:jar"
      And I should get back "org.group:artifact4:4:jar"
      And I should get back "org.group:artifact2:2-SNAPSHOT:jar"
      And I should get back "org.group:artifact:1.0-SNAPSHOT:jar"

  Scenario: Resolve simple SNAPSHOT artifact from remote repository
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back "org.group:artifact:1.0-SNAPSHOT:jar"

  Scenario: Resolve simple SNAPSHOT artifact from remote repository with good maven-metadata.xml digest
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml.sha1" as "7052ca43d9d80806a6148dcfe08ac62b74173016"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back "org.group:artifact:1.0-SNAPSHOT:jar"

  Scenario: Resolve simple SNAPSHOT artifact from remote repository with bad maven-metadata.xml digest
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml.sha1" as "WRONG-SHA1"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get exception when I try to resolve "org.group:artifact:1.0:jar". Exception string "Cannot resolve org.group:artifact:1.0-SNAPSHOT:jar in Repository[Test Remote Repo(remote),memory://remote/]".

  Scenario: Resolve SNAPSHOT artifact from several remote repositories descending order
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-1"
      And I have remote repository "remote2", "Second Test Remote Repo" with uri "memory://remote2/"
      And I have "org.group:artifact:1.0-20100925.124535-2:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20100925.124535</timestamp><buildNumber>2</buildNumber></snapshot><lastUpdated>20100925124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20100925.124535-2.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-2"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back artifact "org.group:artifact:1.0-SNAPSHOT:jar" with content "org.group:artifact:1.0-SNAPSHOT:jar-1"

  Scenario: Resolve SNAPSHOT artifact from several remote repositories ascending order
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-1"
      And I have remote repository "remote2", "Second Test Remote Repo" with uri "memory://remote2/"
      And I have "org.group:artifact:1.0-20101125.124535-2:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101125.124535</timestamp><buildNumber>2</buildNumber></snapshot><lastUpdated>20101125124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101125.124535-2.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-2"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back artifact "org.group:artifact:1.0-SNAPSHOT:jar" with content "org.group:artifact:1.0-SNAPSHOT:jar-2"

  Scenario: Resolve SNAPSHOT artifact from several remote repositories descending order in local repository
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-1"
      And I have remote repository "remote2", "Second Test Remote Repo" with uri "memory://remote2/"
      And I have "org.group:artifact:1.0-20100925.124535-2:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20100925.124535</timestamp><buildNumber>2</buildNumber></snapshot><lastUpdated>20100925124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20100925.124535-2.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-2"
      And I use local repository
      And I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-3"
      And I set file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" timestamp to "20100825124535"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back artifact "org.group:artifact:1.0-SNAPSHOT:jar" with content "org.group:artifact:1.0-SNAPSHOT:jar-1"

  Scenario: Resolve SNAPSHOT artifact from several remote repositories ascending order in local repository
    Given I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-1"
      And I have remote repository "remote2", "Second Test Remote Repo" with uri "memory://remote2/"
      And I have "org.group:artifact:1.0-20101125.124535-2:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101125.124535</timestamp><buildNumber>2</buildNumber></snapshot><lastUpdated>20101125124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101125.124535-2.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-2"
      And I use local repository
      And I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-3"
      And I set file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" timestamp to "20101225124535"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back artifact "org.group:artifact:1.0-SNAPSHOT:jar" with content "org.group:artifact:1.0-SNAPSHOT:jar-3"

  Scenario: Resolve SNAPSHOT artifact from several remote repositories descending order in (first) local repository
    Given I use local repository
      And I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-3"
      And I set file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" timestamp to "20100825124535"
      And I use remote repository
      And I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-1"
      And I have remote repository "remote2", "Second Test Remote Repo" with uri "memory://remote2/"
      And I have "org.group:artifact:1.0-20100925.124535-2:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20100925.124535</timestamp><buildNumber>2</buildNumber></snapshot><lastUpdated>20100925124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20100925.124535-2.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-2"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back artifact "org.group:artifact:1.0-SNAPSHOT:jar" with content "org.group:artifact:1.0-SNAPSHOT:jar-1"

  Scenario: Resolve SNAPSHOT artifact from several remote repositories ascending order in (first) local repository
    Given I use local repository
      And I have "org.group:artifact:1.0-SNAPSHOT:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-3"
      And I set file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar" timestamp to "20101225124535"
      And I use remote repository
      And I have "org.group:artifact:1.0-20101025.124535-3:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101025.124535</timestamp><buildNumber>3</buildNumber></snapshot><lastUpdated>20101025124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101025.124535-3.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-1"
      And I have remote repository "remote2", "Second Test Remote Repo" with uri "memory://remote2/"
      And I have "org.group:artifact:1.0-20101125.124535-2:jar" pom with no dependencies
      And I define file "org/group/artifact/1.0-SNAPSHOT/maven-metadata.xml" as "<?xml version="1.0" encoding="UTF-8"?><metadata><groupId>org.group</groupId><artifactId>artifact</artifactId><version>1.0-SNAPSHOT</version><versioning><snapshot><timestamp>20101125.124535</timestamp><buildNumber>2</buildNumber></snapshot><lastUpdated>20101125124535</lastUpdated></versioning></metadata>"
      And I define file "org/group/artifact/1.0-SNAPSHOT/artifact-1.0-20101125.124535-2.jar" as "org.group:artifact:1.0-SNAPSHOT:jar-2"
     When I ask to resolve "org.group:artifact:1.0-SNAPSHOT:jar"
     Then I should get back artifact "org.group:artifact:1.0-SNAPSHOT:jar" with content "org.group:artifact:1.0-SNAPSHOT:jar-3"

"""


    val NL = "\r\n"
    var now: Long = 0

    var currentRepository: RepositoryInstance = null
    var currentTransport: InMemoryRepository = null
    
    val localTransport = new InMemoryRepository
    
    val localRepository = MavenRepository("local", "Test Local Repo", new URI("memory://local/"), true, true, true, localTransport)

    // var remoteTransport: InMemoryRepository = null
    // val remoteRepositories = List(remoteRepository)
    // remoteRepositories += remoteRepository
    val remoteRepositories = new ArrayBuffer[MavenRepository]
    
    val resolved = new ListBuffer[ArtifactInstance]

    var expecting = false 

    def createPOMXML(artifact: Artifact): StringBuilder = {
        val pomXML = new StringBuilder
        pomXML.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">").append(NL)
        pomXML.append("  <artifactId>").append(artifact.artifactId).append("</artifactId>").append(NL)
        pomXML.append("  <groupId>").append(artifact.groupId).append("</groupId>").append(NL)
        pomXML.append("  <packaging>").append(artifact.typ).append("</packaging>").append(NL)
        pomXML.append("  <name>").append(artifact.toString).append("</name>").append(NL)
        if (artifact.version != null) {
            pomXML.append("  <version>").append(artifact.version).append("</version>").append(NL)
        }
        pomXML.append("  <description>").append(artifact.toString).append("</description>").append(NL)
        pomXML
    }
    
    Given ("cleared all repositories") { () =>
        expecting = false
        // println("     clearing " + resolved.size + " local=" + localTransport.files.size + " remote=" + localTransport.files.size)
        resolved.clear
        // remoteTransport.clear
        localTransport.clear
        
        remoteRepositories.clear
//        val remoteRepository = new MavenRepository("remote", "Test Remote Repo", new URI("memory://remote/"), true, true, false /* pretent as if local so we can easily 'install' stuff to it */, remoteTransport) {
//            override protected def installArtifactPath(artifact: Artifact, artifactFullPath: String) = artifactFullPath
//        }
//        remoteRepositories += remoteRepository
        // println("---- cleared " + resolved.size + " local=" + localTransport.files.size + " remote=" + localTransport.files.size)
    }
    
    Given ("I have remote repository \"(.*)\", \"(.*)\" with uri \"(.*)\"") {
        (id: String, name: String, uriString: String) =>
        
        val remoteTransport = new InMemoryRepository
        val remoteRepository = new MavenRepository(id, name, new URI(uriString), true, true, false /* pretent as if local so we can easily 'install' stuff to it */, remoteTransport) {
            override protected def installArtifactPath(artifact: Artifact, artifactFullPath: String) = artifactFullPath
        }
        remoteRepositories += remoteRepository
        currentRepository = remoteRepository
        currentTransport = remoteTransport
    }
    
    Given ("I use remote repository") { () =>
        currentRepository = remoteRepositories.last
        currentTransport = currentRepository.asInstanceOf[MavenRepository].transport.asInstanceOf[InMemoryRepository]
    }
    
    Given ("I use local repository") { () =>
        currentRepository = localRepository
        currentTransport = currentRepository.asInstanceOf[MavenRepository].transport.asInstanceOf[InMemoryRepository]
    }
    
    Given("I set \"(.*)\" as parent to \"(.*)\"") { (parentArtifactString: String, artifactString: String) =>
        val parentArtifact = MavenArtifact(parentArtifactString)
        val artifact = MavenArtifact(artifactString)
        val artifactInstance = currentRepository.find(MavenArtifact.toPOM(artifact))
        val inputStream = artifactInstance.stream 
        try {
            val b = new Array[Byte](inputStream.available)
            inputStream.read(b)
            val lines = new String(b).split("\r\n")
            val res = new StringWriter
            res.write(lines(0))
            res.write(NL)
            res.write(lines(1))
            res.write(NL)
            res.write("  <parent>" + NL)
            res.write("    <groupId>" + parentArtifact.groupId + "</groupId>" + NL)
            res.write("    <artifactId>" + parentArtifact.artifactId + "</artifactId>" + NL)
            res.write("    <version>" + parentArtifact.version + "</version>" + NL)
            res.write("  </parent>" + NL)
            for (i <- 2 until lines.size) {
                res.write(lines(i))
                res.write(NL)
            }
            // println(res.toString)
            currentRepository.install(new ByteArrayArtifactInstance(res.toString.getBytes, MavenArtifact.toPOM(artifact)))
        } finally {
            inputStream.close
        }
    }

    Given ("I have \"(.*)\" pom with no dependencies") { artifactString: String =>
        val artifact = MavenArtifact(artifactString)
        val pomXML = createPOMXML(artifact)
        pomXML.append("</project>").append(NL)
        currentRepository.install(new ByteArrayArtifactInstance(pomXML.toString().getBytes, MavenArtifact.toPOM(artifact)))
    }
    
    Given ("I have \"(.*)\" pom with dependencies") { (artifactString: String, table: Table) =>
        val artifact = MavenArtifact(artifactString)
        val pomXML = createPOMXML(artifact)
        
        pomXML.append("  <dependencies>").append(NL)
        for (r <- table.rows) {
            val depArtifact = MavenArtifact(r(0))
            pomXML.append("    <dependency>").append(NL)
            pomXML.append("      <artifactId>").append(depArtifact.artifactId).append("</artifactId>")
            pomXML.append("      <groupId>").append(depArtifact.groupId).append("</groupId>")
            pomXML.append("      <type>").append(depArtifact.typ).append("</type>")
            if (depArtifact.version != null) {
                pomXML.append("      <version>").append(depArtifact.version).append("</version>")
            }
            pomXML.append("    </dependency>").append(NL)
        }
        pomXML.append("  </dependencies>").append(NL)
        pomXML.append("</project>").append(NL)
        currentRepository.install(new ByteArrayArtifactInstance(pomXML.toString().getBytes, MavenArtifact.toPOM(artifact)))
    }
    
    Given ("I have \"(.*)\" pom with template dependencies") { (artifactString: String, table: Table) =>
        val artifact = MavenArtifact(artifactString)
        val pomXML = createPOMXML(artifact)
        
        pomXML.append("  <dependencyManagement>").append(NL)
        pomXML.append("  <dependencies>").append(NL)
        for (r <- table.rows) {
            val depArtifact = MavenArtifact(r(0))
            pomXML.append("    <dependency>").append(NL)
            pomXML.append("      <artifactId>").append(depArtifact.artifactId).append("</artifactId>")
            pomXML.append("      <groupId>").append(depArtifact.groupId).append("</groupId>")
            pomXML.append("      <type>").append(depArtifact.typ).append("</type>")
            pomXML.append("      <version>").append(depArtifact.version).append("</version>")
            pomXML.append("    </dependency>").append(NL)
        }
        pomXML.append("  </dependencies>").append(NL)
        pomXML.append("  </dependencyManagement>").append(NL)
        pomXML.append("</project>").append(NL)
        currentRepository.install(new ByteArrayArtifactInstance(pomXML.toString().getBytes, MavenArtifact.toPOM(artifact)))
    }
    
    Given ("I have artifact \"(.*)\"") { artifactString: String =>
        val artifact = MavenArtifact(artifactString)
        val bodyStream = new ByteArrayOutputStream
        bodyStream.write(artifact.toString.getBytes)
        currentRepository.install(new ByteArrayArtifactInstance(bodyStream.toByteArray, artifact))
    }

    When ("I define file \"(.*)\" as \"(.*)\"") {
        (path: String, value: String) =>
        
            currentTransport.copy(new Transport.FileStreamImpl(new ByteArrayInputStream(value.getBytes)) {
                def size = value.length
                def lastModified = null
            }, path)
    }

    When ("I set file \"(.*)\" timestamp to \"(.*)\"") {
        (path: String, timestampString: String) =>
        
        val timestampDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").parse(timestampString)
        
        currentTransport.lastModifiedTimestamps.put(path, timestampDate)
    }
    
    When ("I ask to resolve \"(.*)\"") { artifactString: String => 
        val artifact = MavenArtifact(artifactString)
        val callback: (ArtifactInstance => Unit) = {
            resolvedArtifact: ArtifactInstance =>
                if (!expecting) {
                    println("*** Received response out of bounds!")
                }
                resolved += (resolvedArtifact)
                
        }
        expecting = true
        MavenRepository.resolveTo(artifact, remoteRepositories, localRepository, 
                new ResolutionCallback {
                        def control(a: Artifact) = true
                        def finished(ai: ArtifactInstance) = callback(ai)
                })
    }

    Then ("I should get exception when I try to resolve \"(.*)\". Exception string \"(.*)\".") {
        (artifactString: String, errorString: String) =>
        val artifact = MavenArtifact(artifactString)
        val callback: (ArtifactInstance => Unit) = {
            resolvedArtifact: ArtifactInstance =>
        }
        expecting = true
        try {
            MavenRepository.resolveTo(artifact, remoteRepositories, localRepository, 
                new ResolutionCallback {
                        def control(a: Artifact) = true
                        def finished(ai: ArtifactInstance) = callback(ai)
                })
            fail("Should have received exception by now")
        } catch {
            case x: Exception => assertEquals(errorString, x.getMessage)
        }
    }
    
    Then ("I should get back artifact \"(.*)\" with content \"(.*)\"") { (artifactString: String, content: String) =>
        val artifact = MavenArtifact(artifactString)
        val top = resolved(0)
        resolved.remove(0)
        val got = Artifact(top.artifact)
        assertEquals(artifact, got)

        val inputStream = top.stream 
        try {
            val bodyStream = new ByteArrayOutputStream
            val buffer = new Array[Byte](10240)
            var r = inputStream.read(buffer)
            while (r > 0) {
                bodyStream.write(buffer, 0, r)
                r = inputStream.read(buffer)
            }
            inputStream.close
            assertArrayEquals("Expected " + content.toString + " but got " + new String(bodyStream.toByteArray), 
                    content.toString.getBytes, bodyStream.toByteArray)
        } finally {
            inputStream.close
        }
    
    }

    Then ("I should get back \"(.*)\"") { artifactString: String =>
        val artifact = MavenArtifact(artifactString)
        val top = resolved(0)
        resolved.remove(0)
        val got = Artifact(top.artifact)
        assertEquals(artifact, got)

        val inputStream = top.stream 
        try {
            val bodyStream = new ByteArrayOutputStream
            val buffer = new Array[Byte](10240)
            var r = inputStream.read(buffer)
            while (r > 0) {
                bodyStream.write(buffer, 0, r)
                r = inputStream.read(buffer)
            }
            inputStream.close
            val art = Artifact(top.artifact)
            // println("   Received: " + art.toString)
            // println("A: " + art.toString)
            // println("B: " + new String(bodyStream.toByteArray))
            assertArrayEquals("Expected " + art.toString + " but got " + new String(bodyStream.toByteArray), 
                    art.toString.getBytes, bodyStream.toByteArray)
        } finally {
            inputStream.close
        }
    
    }

}