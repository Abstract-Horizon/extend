package org.abstracthorizon.extend.repository.maven

import org.junit.Assert._
import org.junit.Before
import org.ah.tzatziki.JUnitFeature
import org.ah.tzatziki.Table

import scala.xml.XML 
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer


import java.io._
import java.net._

import org.abstracthorizon.extend.repository._

class TestMavenRepositoryArtifactPathCalculations extends JUnitFeature {

val feature = 
"""
Feature: Maven Repository Resolve One
  Background: something that is executed always
    Given cleared all repositories


  Scenario: Final (non-snapshot) artifact
    Given I have non snapshot repository
      And I have file "org/group/artifactId/1.0/artifactId-1.0.jar" as 
        ""
        /org/group/artifactId/1.0/artifactId-1.0.jar
        ""
     When I invoke find for "org.group:artifactId:1.0:jar"
     Then I should receive request for file "org/group/artifactId/1.0/artifactId-1.0.jar"

  Scenario: Non final (snapshot) artifact
    Given I have snapshot repository
      And I have file "org/group/artifactId/1.2-SNAPSHOT/maven-metadata.xml" as
        ""
<?xml version="1.0" encoding="UTF-8"?> 
<metadata> 
  <groupId>org.ah.tzatziki</groupId> 
  <artifactId>tzatziki-scala</artifactId> 
  <version>1.2-SNAPSHOT</version> 
  <versioning> 
    <snapshot> 
      <timestamp>20101025.124535</timestamp> 
      <buildNumber>3</buildNumber> 
    </snapshot> 
    <lastUpdated>20101025124536</lastUpdated> 
  </versioning> 
</metadata> 
        "" 
      And I have file "org/group/artifactId/1.2-SNAPSHOT/artifactId-1.2-20101025.124535-3.jar" as 
        ""
        org/group/artifactId/1.2/artifactId-1.2-SNAPSHOT.jar
        ""
     When I invoke find for "org.group:artifactId:1.2-SNAPSHOT:jar"
     Then I should receive request for file "org/group/artifactId/1.2-SNAPSHOT/maven-metadata.xml"
      And I should receive request for file "org/group/artifactId/1.2-SNAPSHOT/artifactId-1.2-20101025.124535-3.jar"

"""

    val values = new HashMap[String, Array[Byte]]
    val requestedPaths = new ListBuffer[String]
    
    val testTransport = new Transport {
        def open(path: String): Transport.FileStream = {
            requestedPaths += path
            values.get(path) match {
                case Some(byte) => {
                    new Transport.FileStreamImpl(new ByteArrayInputStream(byte)) {
                        def size = byte.length
                        def lastModified = null
                    }
                }
                case None => throw new FileNotFoundException(path)
            }
        }
        def copy(inputStream: Transport.FileStream, path: String): Unit = null    
                
        def delete(path: String) = new UnsupportedOperationException
    }

    val repositories = new ArrayBuffer[RepositoryInstance]
    val dummyDestination = new ReflectiveRepositoryInstance

    Given("cleared all repositories") { () =>
        requestedPaths.clear
        repositories.clear
    }
    
    Given ("I have non snapshot repository") { () =>
        val repository = MavenRepository("id", "name", new URI("local://here/"), true, true, true, testTransport)
        repositories += repository
    }

    Given ("I have snapshot repository") { () =>
        val repository = MavenRepository("id", "name", new URI("local://here/"), true, true, false, testTransport)
        repositories += repository
    }

    Given ("I have file \"(.*)\" as") { (path: String, content: String) => 
        values.put(path, content.getBytes)
    }

    When ("I invoke find for \"(.*)\"") { artifact: String =>
          val resultArtifactInstance = MavenRepository.ensureIn(MavenArtifact(artifact), repositories, dummyDestination)
          assertNotNull(resultArtifactInstance)
    }
    
    Then ("I should receive request for file \"(.*)\"") { path: String =>
        val top = requestedPaths(0)
        requestedPaths.remove(0)
        assertEquals(path, top)
    }

}