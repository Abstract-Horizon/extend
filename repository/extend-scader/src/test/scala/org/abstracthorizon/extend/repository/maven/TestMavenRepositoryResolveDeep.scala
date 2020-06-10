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

class TestMavenRepositoryResolveDeep extends JUnitFeature {

var feature = 
"""
Feature: Maven Repository Resolve Deep

  Background: something that is executed always
    Given cleared all repositories

  Scenario: Resolve deep for simple POM
    Given I have "org.group:artifact:1.0:jar" pom with no dependencies
      And I have artifact "org.group:artifact:1.0:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact:1.0:jar"

  Scenario: Resolve deep for POM with dependencies
    Given I have "org.group:artifact:1.0:jar" pom with dependencies
       |dependency|
       |org.group:artifact2:2:jar|
       |org.group:artifact3:3:jar|
      And I have artifact "org.group:artifact:1.0:jar"
      And I have artifact "org.group:artifact2:2:jar"
      And I have artifact "org.group:artifact3:3:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact2:2:jar"
      And I should get back "org.group:artifact3:3:jar"
      And I should get back "org.group:artifact:1.0:jar"


  Scenario: Resolve deep for POM with parent and dependencies
    Given I have "org.group:parent-artifact:1.0:jar" pom with template dependencies
       |dependency|
       |org.group:artifact2:2:jar|
       |org.group:artifact3:3:jar|
    Given I have "org.group:artifact:1.0:jar" pom with dependencies
       |dependency|
       |org.group:artifact2::jar|
       |org.group:artifact3::jar|
      And I set "org.group:parent-artifact:1.0:jar" as parent to "org.group:artifact:1.0:jar"
      And I have artifact "org.group:artifact:1.0:jar"
      And I have artifact "org.group:artifact2:2:jar"
      And I have artifact "org.group:artifact3:3:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact2:2:jar"
      And I should get back "org.group:artifact3:3:jar"
      And I should get back "org.group:artifact:1.0:jar"


  Scenario: Resolve deep for POM with transitive dependencies
    Given I have "org.group:artifact:1.0:jar" pom with dependencies
       |dependency|
       |org.group:artifact2:2:jar|
       |org.group:artifact3:3:jar|
      And I have "org.group:artifact2:2:jar" pom with dependencies
       |dependency|
       |org.group:artifact4:4:jar|
      And I have artifact "org.group:artifact:1.0:jar"
      And I have artifact "org.group:artifact2:2:jar"
      And I have artifact "org.group:artifact3:3:jar"
      And I have artifact "org.group:artifact4:4:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact4:4:jar"
      And I should get back "org.group:artifact2:2:jar"
      And I should get back "org.group:artifact3:3:jar"
      And I should get back "org.group:artifact:1.0:jar"


  Scenario: Resolve deep for POM with shared transitive dependencies
    Given I have "org.group:artifact:1.0:jar" pom with dependencies
       |dependency|
       |org.group:artifact2:2:jar|
       |org.group:artifact3:3:jar|
      And I have "org.group:artifact2:2:jar" pom with dependencies
       |dependency|
       |org.group:artifact3:3:jar|
       |org.group:artifact4:4:jar|
      And I have artifact "org.group:artifact:1.0:jar"
      And I have artifact "org.group:artifact2:2:jar"
      And I have artifact "org.group:artifact3:3:jar"
      And I have artifact "org.group:artifact4:4:jar"
     When I ask to resolve "org.group:artifact:1.0:jar"
     Then I should get back "org.group:artifact3:3:jar"
      And I should get back "org.group:artifact4:4:jar"
      And I should get back "org.group:artifact2:2:jar"
      And I should get back "org.group:artifact:1.0:jar"


  Scenario: Resolve deep for POM with recursive, cyclic reference
    Given I have "org.group:artifact:1.0:jar" pom with dependencies
       |dependency|
       |org.group:artifact2:2:jar|
       |org.group:artifact3:3:jar|
      And I have "org.group:artifact2:2:jar" pom with dependencies
       |dependency|
       |org.group:artifact:1.0:jar|
       |org.group:artifact4:4:jar|
      And I have artifact "org.group:artifact:1.0:jar"
      And I have artifact "org.group:artifact2:2:jar"
      And I have artifact "org.group:artifact3:3:jar"
      And I have artifact "org.group:artifact4:4:jar"
     Then I should get exception when I try to resolve "org.group:artifact:1.0:jar". Exception string "Got to circular reference: Dependency[org.group:artifact:1.0:jar, scope=null, optional=false] is being resolved. Stack [org.group:artifact2:2:jar, org.group:artifact:1.0:jar]".


  Scenario: Resolve deep for simple POM with wrong SHA1 digest
    Given I have "org.group:artifact:1.0:jar" pom with no dependencies
      And I have artifact "org.group:artifact:1.0:jar"
     When I define file "org/group/artifact/1.0/artifact-1.0.jar.sha1" as "WRONG"
     Then I should get exception when I try to resolve "org.group:artifact:1.0:jar". Exception string "Cannot resolve org.group:artifact:1.0-SNAPSHOT:jar in Repository[Test Remote Repo(remote),memory://remote/]".

"""

    val NL = "\r\n"
    var now: Long = 0

    val remoteTransport = new InMemoryRepository
    val localTransport = new InMemoryRepository
    val remoteRepository = MavenRepository("remote", "Test Remote Repo", new URI("memory://remote/"), true, true, true /* pretend as if being local so we can easily 'install' stuff to it */, remoteTransport)
    val localRepository = MavenRepository("local", "Test Local Repo", new URI("memory://local/"), true, true, true, localTransport)

    // val remoteRepositories = List(remoteRepository)
    val remoteRepositories = new ArrayBuffer[MavenRepository]
    remoteRepositories += remoteRepository
    
    val resolved = new ListBuffer[Artifact]

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
        remoteTransport.clear
        localTransport.clear
        
        remoteRepositories.clear
        val remoteRepository = MavenRepository("remote", "Test Remote Repo", new URI("memory://remote/"), true, true, true /* pretent as if local so we can easily 'install' stuff to it */, remoteTransport)
        remoteRepositories += remoteRepository
        // println("---- cleared " + resolved.size + " local=" + localTransport.files.size + " remote=" + localTransport.files.size)
    }

    Given("I set \"(.*)\" as parent to \"(.*)\"") { (parentArtifactString: String, artifactString: String) =>
        val parentArtifact = MavenArtifact(parentArtifactString)
        val artifact = MavenArtifact(artifactString)
        val artifactInstance = remoteRepository.find(MavenArtifact.toPOM(artifact))
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
            remoteRepository.install(new ByteArrayArtifactInstance(res.toString.getBytes, MavenArtifact.toPOM(artifact)))
        } finally {
            inputStream.close
        }
    }

    Given ("I have \"(.*)\" pom with no dependencies") { artifactString: String =>
        val artifact = MavenArtifact(artifactString)
        val pomXML = createPOMXML(artifact)
        pomXML.append("</project>").append(NL)
        remoteRepository.install(new ByteArrayArtifactInstance(pomXML.toString().getBytes, MavenArtifact.toPOM(artifact)))
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
        remoteRepository.install(new ByteArrayArtifactInstance(pomXML.toString().getBytes, MavenArtifact.toPOM(artifact)))
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
        remoteRepository.install(new ByteArrayArtifactInstance(pomXML.toString().getBytes, MavenArtifact.toPOM(artifact)))
    }
    
    Given ("I have artifact \"(.*)\"") { artifactString: String =>
        val artifact = MavenArtifact(artifactString)
        val bodyStream = new ByteArrayOutputStream
        bodyStream.write(artifact.toString.getBytes)
        remoteRepository.install(new ByteArrayArtifactInstance(bodyStream.toByteArray, artifact))
    }

    When ("I define file \"(.*)\" as \"(.*)\"") {
        (path: String, value: String) =>
        
            remoteTransport.copy(new Transport.FileStreamImpl(new ByteArrayInputStream(value.getBytes)) {
                def size = value.length
                def lastModified = null
            }, path)
    }

    
    When ("I ask to resolve \"(.*)\"") { artifactString: String => 
        val artifact = MavenArtifact(artifactString)
        val callback: (ArtifactInstance => Unit) = {
            resolvedArtifact: ArtifactInstance =>
                if (!expecting) {
                    println("*** Received response out of bounds!")
                }
                resolved += (resolvedArtifact.artifact)
                val inputStream = resolvedArtifact.stream 
                try {
                    val bodyStream = new ByteArrayOutputStream
                    val buffer = new Array[Byte](10240)
                    var r = inputStream.read(buffer)
                    while (r > 0) {
                        bodyStream.write(buffer, 0, r)
                        r = inputStream.read(buffer)
                    }
                    inputStream.close
                    val art = Artifact(resolvedArtifact.artifact)
                    // println("   Received: " + art.toString)
                    // println("A: " + art.toString)
                    // println("B: " + new String(bodyStream.toByteArray))
                    assertArrayEquals("Expected " + art.toString + " but got " + new String(bodyStream.toByteArray), 
                            art.toString.getBytes, bodyStream.toByteArray)
                } finally {
                    inputStream.close
                }
                
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
    
    Then ("I should get back \"(.*)\"") { artifactString: String =>
        val artifact = MavenArtifact(artifactString)
        val top = resolved(0)
        resolved.remove(0)
        val got = Artifact(top)
        assertEquals(artifact, got)
    }
}