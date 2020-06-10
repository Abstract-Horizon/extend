package org.abstracthorizon.extend.repository.maven

import org.junit.Assert._
import org.ah.tzatziki.JUnitFeature
import org.ah.tzatziki.Table

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.xml.XML 

import java.io._
import java.net._

import org.abstracthorizon.extend.repository._

class TestDependencyResolver extends JUnitFeature {
    
val feature = 
"""
Feature: Loading POM

  Scenario outline: Load parent POM with template dependencies 
                    through dependency resolving

    Given I map "org.groupid:child-artifact-id:0.2:pom" to resource "child-pom-with-dependencies.xml"
      And I map "org.groupid:parent-artifact-id:0.1p:pom" to resource "parent-pom-with-template-dependencies.xml"
     When I ask for "org.groupid:child-artifact-id:0.2:pom"
     Then I should have <what> in <where>

      Examples:
         | what | where |
         | "org.groupid" | "parentPOM.artifact.groupId" | 
         | "parent-artifact-id" | "parentPOM.artifact.artifactId" | 
         | "0.1p" | "parentPOM.artifact.version" |
         | "org.groupId1" | "dependencies(0).groupId" | 
         | "artifactId1" | "dependencies(0).artifactId" | 
         | "version1" | "dependencies(0).version" |
         | null | "dependencies(0).typ" |
         | null | "dependencies(0).classifier" |
         | null | "dependencies(0).scope" |
         | false | "dependencies(0).optional" |
         | "org.groupId2" | "dependencies(1).groupId" | 
         | "artifactId2" | "dependencies(1).artifactId" | 
         | "version2" | "dependencies(1).version" |
         | "type2" | "dependencies(1).typ" |
         | "classifier2" | "dependencies(1).classifier" | 
         | "scope2" | "dependencies(1).scope" |
         | true | "dependencies(1).optional" |
         | "version3" | "dependencies(2).version" | 
         | "type3" | "dependencies(2).typ" |
         | "classifier3" | "dependencies(2).classifier" | 
         | "scope3" | "dependencies(2).scope" |
         | "artifactId3" | "dependencies(2).artifactId" |
         | false | "dependencies(2).optional" |


"""
    
    val map = new HashMap[String, String]
    val pom: POM = null

    val repositories = new ArrayBuffer[Repository]
    
    repositories += new MavenRepository("testrepo", "Test Repository", new URI("test:/"), true, true, true,
            new AbstractURLTransport {
                def resourceURL(path: String): URL = {
                    val p = map.getOrElse(path, null)
                    var url = getClass().getResource(p)
                    if (url == null) {
                        url = Thread.currentThread.getContextClassLoader.getResource(p)
                    }
                    if (url != null) {
                        url
                    } else {
                        throw new FileNotFoundException(url.toString())
                    }
                }
            }
    )

    
    When("I ask for \"(.*)\"") { artifact: String =>
        // MavenDependencyResolver.resolveDependencies(FullArtifact(artifact), repositories, null)
    }

    Given("I map \"(.*)\" to resource \"(.*)\"") { (artifact: String, resource: String) =>
        map.put(MavenRepository.defaultArtifactPath(MavenArtifact(artifact)), resource)
    }

    Given("I load \"(.*)\" pom from the classpath") { value: String => 

    }



    Then("I should have (true|false) in \"(.*)\"") { (value: Boolean, path: String) =>
       val res = BeanHelper.read(pom, path)
       assertEquals(value, res)
    }

    
    Then("I should have null in \"(.*)\"") { path: String =>
       val res = BeanHelper.read(pom, path)
       assertNull(res)
    }

    Then("I should have not null in \"(.*)\"") { path: String =>
       val res = BeanHelper.read(pom, path)
       assertNotNull(res)
    }

    Then("I should have \"(.*)\" in \"(.*)\"") { (value: String, path: String) =>
       val res = BeanHelper.read(pom, path)
       assertEquals(value, res)
    }
}