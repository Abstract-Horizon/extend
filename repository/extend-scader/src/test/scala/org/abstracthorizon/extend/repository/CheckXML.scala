package org.abstracthorizon.extend.repository

import scala.xml._

object CheckXML {

    def main(args: Array[String]) = {
        
        
        val xml = XML.loadString("<node>" +
                "<list><x1>A</x1><x2>B</x2><x3>C</x3></list>" +
                "<deps><dep><name>x</name></dep><dep><name>y</name></dep></deps>" +
                "<temp-deps><dep><name>A</name></dep><dep><name>B</name></dep></temp-deps>" +
        		"<sub><t> xxx\n\n</t></sub><sub><t>xxx2</t></sub><unique>qwe</unique></node>")

        val list = (xml \\ "list")
        for (l <- list) {
            println(": " + l)
            for (elem <- l.child) {
                println("::  " + elem)
                println("   l=" + elem.label)
                println("   t=" + elem.text)
            }
        }
        
        println()
        println()
        println("Deps:")
        val deps = (xml \ "deps" \ "dep")
        for (dep <- deps) {
            println(":    " + dep)
            println("   n=" + (dep \ "name").text)
        }
        
        println()
        println()
        println("Temp Deps:")
        val tempDeps = (xml \ "temp-deps" \ "dep")
        for (dep <- tempDeps) {
            println(":    " + dep)
        }
        
        println()
        println()
        val unique = (xml \\ "unique").text
        println("unique: " + unique)
        
        val notthere = (xml \\ "notthere").text
        println("notthere: '" + notthere + "', " + notthere.length + ", " + (notthere == null))
        
        if (xml.contains("asadas")) {
            val unexistingTag = (xml \\ "asdasd")
            println(unexistingTag != null)
        } else {
            println("Doesn't contain it!")
        }
        
        println("-----------------------------------------------------------------------------------")
        
        val xmlString2 = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">" + 
                         "  <artifactId>artifact-id</artifactId>" + 
                         "  <groupId>org.groupid</groupId>" + 
                         "  <packaging>packaging</packaging>" + 
                         "  <name>Name of the Artifact</name>" + 
                         "  <version>0.1</version>" + 
                         "  <description>Description</description>" + 
                         "</project>"
        
        val xml2 = XML.loadString(xmlString2)                         

        println("1: " + (xml2 \ "project").text)
        println("2: " + (xml2 \ "project" \ "name").text)
        println("3: " + (xml2 \ "name").text)
        println("4: " + (xml2 \\ "name").text)
        println("5: " + ((xml2 \ "project").contains("name")))
        xml2 \ "name" match {
            case e: Elem => println("6a: contains name " + e)
            case n: NodeSeq => println("6b: contains name " + n)
            case _ => println("6c: doesn't contains name")
        }
        println("7a: " + ((xml2 \ "name").size))
        println("7b: " + ((xml2 \ "nameX").size))
        
        println("-----------------------------------------------------------------------------------")

        val lastName = (xml \\ "name" last).text.trim
        
        println("lastname: " + lastName)
        
        println("End.")
    }
    
}