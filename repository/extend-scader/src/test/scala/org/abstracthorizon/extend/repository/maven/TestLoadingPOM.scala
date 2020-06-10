package org.abstracthorizon.extend.repository.maven

import org.junit.Assert._
import org.ah.tzatziki.JUnitFeature
import org.ah.tzatziki.Table

import scala.xml.XML 

import java.io._

class TestLoadingPOM extends JUnitFeature {

val feature = 
"""
Feature: Loading POM

  Scenario: Load simple no dependency pom
    Given I have pom:
      ""
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <artifactId>artifact-id</artifactId>
  <groupId>org.groupid</groupId>
  <packaging>packaging</packaging>
  <name>Name of the Artifact</name>
  <version>0.1</version>
  <description>Description</description>
</project>
      ""
    When I parse that pom
    Then I should have
         | what | where |
         | "artifact-id" | "artifact.artifactId" |
         | "org.groupid" | "artifact.groupId" |
         | "packaging" | "artifact.typ" |
         | "Name of the Artifact" | "name" | 
         | "0.1" | "artifact.version" |
         | "Description" | "description" | 



  Scenario: Load simple pom with dependencies
    Given I load "pom-with-dependencies.xml" pom from the classpath
    When I parse that pom
    Then I should have
         | what | where |
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
         | "artifactId3" | "dependencies(2).artifactId" | 
         | "version3" | "dependencies(2).version" |
         | "type3" | "dependencies(2).typ" |
         | "classifier3" | "dependencies(2).classifier" | 
         | "scope3" | "dependencies(2).scope" |
         | false | "dependencies(2).optional" |



  Scenario: Load parent pom with template dependencies
    Given I load "pom-with-template-dependencies.xml" pom from the classpath
    When I parse that pom
    Then I should have         
         | what | where |
         | "org.groupId1" | "templateDependencies(0).groupId" | 
         | "artifactId1" | "templateDependencies(0).artifactId" | 
         | "version1" | "templateDependencies(0).version" |
         | null | "templateDependencies(0).typ" |
         | null | "templateDependencies(0).classifier" |
         | null | "templateDependencies(0).scope" |
         | false | "templateDependencies(0).optional" |
         | "org.groupId2" | "templateDependencies(1).groupId" | 
         | "artifactId2" | "templateDependencies(1).artifactId" | 
         | "version2" | "templateDependencies(1).version" |
         | "type2" | "templateDependencies(1).typ" |
         | "classifier2" | "templateDependencies(1).classifier" | 
         | "scope2" | "templateDependencies(1).scope" |
         | true | "templateDependencies(1).optional" |
         | "version3" | "templateDependencies(2).version" | 
         | "type3" | "templateDependencies(2).typ" |
         | "classifier3" | "templateDependencies(2).classifier" | 
         | "scope3" | "templateDependencies(2).scope" |
         | "artifactId3" | "templateDependencies(2).artifactId" |
         | false | "templateDependencies(2).optional" |



  Scenario: pom repositories
    Given I load "pom-with-repositories.xml" pom from the classpath
    When I parse that pom
    Then I should have 
         | what | where |
         | "abstracthorizon" | "repositories(0).id" |
         | "Abstracthorizon.org Repository" | "repositories(0).name" | 
         | false | "repositories(0).snapshots" |
         | true | "repositories(0).releases" |
         | "abstracthorizon.snapshot" | "repositories(1).id" | 
         | "Abstracthorizon.org Snapshot Repository" | "repositories(1).name" | 
         | true | "repositories(1).snapshots" |
         | false | "repositories(1).releases" |



  Scenario: pom with properties
    Given I load "pom-with-properties.xml" pom from the classpath
    When I parse that pom
    Then I should have
         | what | where |
         | "org.groupId1" | "dependencies(0).groupId" | 
         | "artifactId1" | "dependencies(0).artifactId" | 
         | "version1" | "dependencies(0).version" |

"""
    
    var pomXMLString: String = null
    var pom: POM = null
     
    Given("I have pom:") { pomString: String =>
        // val pomString: String = ""
        pomXMLString = pomString
    }

    Given("I load \"(.*)\" pom from the classpath") { value: String => 
        var url = getClass().getResource(value)
        if (url == null) {
            url = Thread.currentThread.getContextClassLoader.getResource(value)
        }
        val is = url.openStream
        try {
            val reader = new BufferedReader(new InputStreamReader(is))
            val sb = new StringBuilder
            var line = reader.readLine
            while (line != null) {
                sb.append(line).append('\n')
                line = reader.readLine
            }
            pomXMLString = sb.toString
        } finally {
            is.close()
        }
    }

    When("I parse that pom") { () =>
      pom = POM.load(new ByteArrayInputStream(pomXMLString.getBytes))
    }

    Then("I should have") { table: Table =>
       for (row <- table.rows) {
           Then("I should have " + row(0) + " in " + row(1))
       }
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
       val res = BeanHelper.readAsString(pom, path)
       assertEquals(value, res)
    }
}