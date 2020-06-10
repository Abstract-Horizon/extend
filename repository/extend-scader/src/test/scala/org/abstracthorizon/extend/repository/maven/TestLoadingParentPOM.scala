package org.abstracthorizon.extend.repository.maven

import org.junit.Assert._
import org.ah.tzatziki.JUnitFeature
import org.ah.tzatziki.Table

import scala.xml.XML 

import java.io._

class TestLoadingParentPOM extends JUnitFeature {
    
val feature = 
"""
Feature: Loading POM

  Scenario outline: Load parent pom with template dependencies
    Given I load "parent-pom-with-template-dependencies.xml" pom from the classpath
      And I parse that pom
      And I store it as parent pom
      And I load "child-pom-with-dependencies.xml" pom from the classpath
     When I parse that pom
      And I set parent pom
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
    
    var pomXMLString: String = null
    var pom: POM = null
    var parentPom: POM = null
     
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
      val pomXml = XML.loadString(pomXMLString)
      pom = new POM
      pom.read(pomXml)
    }
    
    When("I store it as parent pom") { () =>
      parentPom = pom
    }
    
    When("I set parent pom") { () =>
      pom.cachedParentPOM = parentPom
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