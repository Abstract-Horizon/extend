package org.abstracthorizon.extend.repository.maven

import org.junit.Assert._
import org.ah.tzatziki.JUnitFeature
import org.ah.tzatziki.Table

import scala.xml.XML 
import scala.collection.mutable.ArrayBuffer

import java.util.Random

import java.io._
import java.net._

import org.abstracthorizon.extend.repository._

class TestMavenRepositoryEnsureIn extends JUnitFeature {

val feature = 
"""
Feature: Maven Repository Find

  Background: something that is executed always
    Given cleared all repositories

  Scenario: Spawn four actors and wait for resutls
    Given I have repositories
        |id|name|uri|releases|snapshots|local|delay|has resource|
        |a|A|uri:/a|true|true|true|100|true|
        |b|B|uri:/b|true|true|true|600|true|
        |c|C|uri:/c|true|true|true|1000|true|
        |d|D|uri:/d|true|true|true|2000|true|
     When I invoke ensureIn for "org.group:artifactId:1.0"
     Then I should receive artifact from repository "a"

  Scenario: Spawn four actors and wait for second to return first
    Given I have repositories
        |id|name|uri|releases|snapshots|local|delay|has resource|
        |a|A|uri:/a|true|true|true|1000|true|
        |b|B|uri:/b|true|true|true|200|true|
        |c|C|uri:/c|true|true|true|1000|true|
        |d|D|uri:/d|true|true|true|2000|true|
     When I invoke ensureIn for "org.group:artifactId:1.0"
     Then I should receive artifact from repository "b"

  Scenario: Spawn four actors and wait for Third to return first
    Given I have repositories
        |id|name|uri|releases|snapshots|local|delay|has resource|
        |a|A|uri:/a|true|true|true|1000|true|
        |b|B|uri:/b|true|true|true|600|true|
        |c|C|uri:/c|true|true|true|100|true|
        |d|D|uri:/d|true|true|true|2000|true|
     When I invoke ensureIn for "org.group:artifactId:1.0"
     Then I should receive artifact from repository "c"

  Scenario: Spawn four actors of which first and last won't return anything and wait for third to return value first
    Given I have repositories
        |id|name|uri|releases|snapshots|local|delay|has resource|
        |a|A|uri:/a|true|true|true|100|false|
        |b|B|uri:/b|true|true|true|1600|true|
        |c|C|uri:/c|true|true|true|1000|true|
        |d|D|uri:/d|true|true|true|2000|false|
     When I invoke ensureIn for "org.group:artifactId:1.0"
     Then I should receive artifact from repository "c"

  Scenario: Spawn four actors of which none will return anything and measure 'parallelness' of it...
    Given I have repositories
        |id|name|uri|releases|snapshots|local|delay|has resource|
        |a|A|uri:/a|true|true|true|500|false|
        |b|B|uri:/b|true|true|true|1000|false|
        |c|C|uri:/c|true|true|true|1500|false|
        |d|D|uri:/d|true|true|true|2000|false|
     When I note the time
      And I invoke ensureIn for "org.group:artifactId:1.0" ignoring exception 
     Then I should not receive artifact from repository 
      And lapsed time must be less than 2200ms



"""
    val randomGenerator = new Random

    val repositories = new ArrayBuffer[RepositoryInstance]
    val dummyDestination = new ReflectiveRepositoryInstance
    var resultArtifactInstance: ArtifactInstance = null
    var now: Long = 0
    var bytes: Array[Byte] = new Array[Byte](10240)
    
    randomGenerator.nextBytes(bytes)
    
    Given("I note the time") { () => 
        // println("Noting time!")
        now = System.currentTimeMillis
    }

    Given("cleared all repositories") { () =>
        repositories.clear
        resultArtifactInstance = null
    }
    
    Given("I have repositories") {
        (table: Table) =>
        
        for (r <- table.rows) {
            val repository = new MavenRepository(r(0), r(1), new URI(r(2)), r(3).equals("true"), r(4).equals("true"), r(5).equals("true"), 
                    new Transport() {
                        def open(path: String): Transport.FileStream = {
                            Thread.sleep(r(6).toInt)
                            if (r(7).equals("true")) {
                                new Transport.FileStreamImpl(new InputStream() {
                                    def read(): Int = 0
                                }) {
                                    def size = 0
                                    def lastModified = null
                                }
                            } else {
                                throw new IOException("Resource not found 404")
                            }
                        }
                        
                        def copy(inputStream: Transport.FileStream, path: String): Unit = {}
                        def delete(path: String) = new UnsupportedOperationException
                    })
           
           repositories += repository
        }
    }


    When ("I invoke ensureIn for \"(.*)\"") {
        (artifact: String) =>
          // println("Invoking for " + artifact)
          resultArtifactInstance = MavenRepository.ensureIn(MavenArtifact(artifact), repositories, dummyDestination)
    }

    When ("I invoke ensureIn for \"(.*)\" ignoring exception") {
        (artifact: String) =>
          // println("Invoking for " + artifact)
        try{
             resultArtifactInstance = MavenRepository.ensureIn(MavenArtifact(artifact), repositories, dummyDestination)
        } catch {
            case ioe: FileNotFoundException =>
            case t: Throwable => throw t
        }
    }

    Then("I should receive artifact from repository \"(.*)\"") { (repositoryId: String) =>
        assertNotNull("Didn't receive response!", resultArtifactInstance)
        assertEquals("Received from wrong repository", repositoryId, resultArtifactInstance.repository.id)
    }
    
    Then("I should not receive artifact from repository") { () =>
        assertNull("Did receive response! " + resultArtifactInstance, resultArtifactInstance)
    }
    		 
    Then("lapsed time must be less than (\\d+)ms") { (total: Long) =>
        val n = System.currentTimeMillis
        // println("Lapsed " + (n - now) + "(" + ((n - now) < total) + ")")
        assertTrue("Lapsed time is more than " + total, ((n - now) < total))
    }
    
}