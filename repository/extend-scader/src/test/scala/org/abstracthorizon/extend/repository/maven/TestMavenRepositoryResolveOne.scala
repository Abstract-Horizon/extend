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

class TestMavenRepositoryResolveOne extends JUnitFeature {

val feature = 
"""
Feature: Maven Repository Resolve One

  Background: something that is executed always
    Given cleared all repositories

  Scenario: Spawn four actors and wait for resutls
    Given I have repositories
        |id|name|uri|releases|snapshots|local|delay|has resource|
        |a|A|uri:/a|true|true|true|100|true|
        |b|B|uri:/b|true|true|true|600|true|
        |c|C|uri:/c|true|true|true|1000|true|
        |d|D|uri:/d|true|true|true|2000|true|
     When I invoke find for "org.group:artifactId:1.0"
     Then I should receive artifact from repository "a"



"""
    val randomGenerator = new Random

    val repositories = new ArrayBuffer[RepositoryInstance]
    var resultArtifactInstance: ArtifactInstance = null
    var now: Long = 0
    var bytes: Array[Byte] = new Array[Byte](10240)
    val dummyDestination = new ReflectiveRepositoryInstance
    
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


    When ("I invoke find for \"(.*)\"") {
        (artifact: String) =>
          // println("Invoking for " + artifact)
          resultArtifactInstance = MavenRepository.ensureIn(MavenArtifact(artifact), repositories, dummyDestination)
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