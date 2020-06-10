package org.abstracthorizon.extend.repository.maven

import scala.collection.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedHashSet
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ArrayBuffer

import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.XML 

import java.io.File
import java.io.InputStream
import java.io.IOException
import java.net.URI
import java.net.URL

import org.abstracthorizon.extend.repository.Artifact
import org.abstracthorizon.extend.repository.ArtifactImpl
import org.abstracthorizon.extend.repository.ArtifactInstance
import org.abstracthorizon.extend.repository.AbstractArtifactInstance
import org.abstracthorizon.extend.repository.Repository
import org.abstracthorizon.extend.repository.RepositoryImpl
import org.abstracthorizon.extend.repository.Log
import org.abstracthorizon.extend.repository.Version

object POM extends Log {

    def load(inputStream: InputStream): POM = {
        val pom = new POM
        val pomXml = XML.load(inputStream)
        pom.read(pomXml)
        pom
    }

    def load(artifactInstance: ArtifactInstance): POM = {
        val inputStream = artifactInstance.stream 
        try {
            load(inputStream)
        } finally {
            if (inputStream != null) {
                inputStream.close
            }
        }
    }
}

class POM extends Log {
    
    private var parentArtifactInt: Artifact = null
    private var artifactInt: Artifact = null
    private var nameInt: String = null
    private var descriptionInt: String = null
    
    protected def updateParentArtifact(parentArtifact: Artifact) = this.parentArtifactInt = parentArtifact
    
    protected[maven] var cachedParentPOM: POM = null

    protected[maven] def parentPOM: POM = {
        if (cachedParentPOM != null) {
            return cachedParentPOM
        }
        return cachedParentPOM
    }
    
    private val deps = new LinkedHashMap[Artifact, Dependency]
    private val templateDeps = new LinkedHashMap[Artifact, Dependency]
    private val repositoriesInt = new LinkedHashMap[String, Repository]

    private var parsedDependencies = false
    private var parsedTemplateDependencies = false
    private var parsedRepositories = false
    private var project: NodeSeq = null
    
    protected val props = new HashMap[String, String]
    
    protected[repository] def read(project: Elem) = {
        this.project = project

        parseProperties(project)

        if ((project \ "parent").size > 0) {
            val parentGroupId = nodeText(project \ "parent" \ "groupId")
            val parentArtifactId = nodeText(project \ "parent" \ "artifactId")
            val parentVersionString = nodeText(project \ "parent" \ "version")
            val parentVersion = MavenVersion(parentVersionString)
            parentArtifactInt = Artifact(parentGroupId, parentArtifactId, parentVersion)
        }
        
        nameInt = nodeText(project \ "name")
        descriptionInt = nodeText(project \ "description")

        var groupId = nodeText(project \ "groupId")
        val artifactId = nodeText(project \ "artifactId")
        var versionString = nodeText(project \ "version")
        val packaging = nodeText(project \ "packaging")
        
        if (groupId == null) {
            groupId = parentArtifactInt.groupId
        }

        val version = if (versionString != null) {
            MavenVersion(versionString)
        } else {
            if (parentPOM != null) {
                parentPOM.artifact.version
            } else {
                null
            }
        }
        
        artifactInt = Artifact(groupId, artifactId, version, packaging)
    }

    protected[maven] def updateParentPOM(parentPOM: POM) = {
        cachedParentPOM = parentPOM
        if (artifactInt.version == null) {
            artifactInt = Artifact(artifactInt.groupId, artifactInt.artifactId, parentPOM.artifactInt.version, artifactInt.typ)
        }
    }
    
    protected def parseProperties(project: NodeSeq) = {
        val properties = project \\ "properties" 
        for (e <- properties)
            e match {
            case e: Elem =>
                for (p <- e.child) {
                    this.props.put(p.label, p.text)
                }
            case x => println("No idea what is this " + x)
        }
    }
    
    protected def parseRepositories() = {
        val repositories = project \ "repositories" \ "repository"
        for (repository <- repositories) {
            val id = nodeText(repository \ "id")
            val name = nodeText(repository \ "name")
            val uriText = nodeText(repository \ "url")
            val releases = "true".equalsIgnoreCase(nodeText(repository \ "releases" \ "enabled"))
            val snapshots = "true".equalsIgnoreCase(nodeText(repository \ "snapshots" \ "enabled"))
            val uri = if (uriText != null) new URI(uriText) else null
            val repositoryImpl = new RepositoryImpl(id, name, uri, releases, snapshots)
            repositoriesInt.put(id, repositoryImpl)
        }
        parsedRepositories = true
        checkRelease
    }
    
    protected def parseDependencies() = {
        val dependencies = project \ "dependencies" \ "dependency"
        for (dependency <- dependencies) {
            val depImpl = parseDependency(dependency, false)
        
            deps.put(Artifact.baseArtifact(depImpl), depImpl)
        }
        parsedDependencies = true
        checkRelease
    }
    
    protected def parseTemplateDependencies() = {
        val dependencies = project \ "dependencyManagement" \ "dependencies" \ "dependency"
        for (dependency <- dependencies) {
            val depImpl = parseDependency(dependency, true)
        
            templateDeps.put(Artifact.baseArtifact(depImpl), depImpl)
        }
        parsedTemplateDependencies = true
        checkRelease
    }
    
    protected def parseDependency(node: NodeSeq, template: Boolean): Dependency = {
        val groupId = nodeText(node \ "groupId")
        val artifactId = nodeText(node \ "artifactId")
        
        val version = nodeText(node \ "version")
        val typ = nodeText(node \ "type")
        val classifier = nodeText(node \ "classifier")
        val scope = nodeText(node \ "scope")

        val dependency = new InternalDependency(template)
        dependency.update(groupId, artifactId, version, typ, classifier)
        dependency.updateScope(scope)
        val op = nodeText(node \ "optional")
        if (op != null) {
            dependency.updateOptional("true".equalsIgnoreCase(op))
        }
        
        dependency
    }
    
    protected def checkRelease = {
        if (parsedDependencies && parsedTemplateDependencies && parsedRepositories) {
            project = null
        }
    }
    
    protected def nodeText(node: NodeSeq): String = {
        if (node.size > 0) {
            val t = node.text.trim
            if (t.indexOf("${") >= 0) {
                val b = new StringBuilder
                b.append(t)

                var i = b.indexOf("${")
                while (i >= 0) {
                    val k = b.indexOf('}', i)
                    if (k < 0) {
                        return b.toString
                    }
                    val name = b.substring(i + 2, k)
                    val value = getProperty(name)
                    if (value != null) {
                        b.replace(i, k + 1, value)
                        i = b.indexOf("${", i + value.length)
                    } else {
                        debug_warn(" *** Failed to find property for " + name + " @ " + artifactInt)
                        i = b.indexOf("${", k + 1)
                    }
                }
                
                b.toString
            } else {
                t
            }
        } else {
            null
        }
    }
    
    protected def getProperty(name: String): String = {
        if ("pom.version".equals(name) || "project.version".equals(name)) {
            return artifactInt.version.toString
        }
        if ("pom.groupId".equals(name) || "project.groupId".equals(name)) {
            return artifactInt.groupId
        }
        if ("pom.artifactId".equals(name) || "project.artifactId".equals(name)) {
            return artifactInt.artifactId
        }
        
        props.get(name) match {
            case Some(value) => value
            case None => if (parentPOM != null) {
                parentPOM.getProperty(name)
            } else {
                null
            }
        }
    }
    
    class InternalDependency(val template: Boolean) extends Dependency with Artifact {
        var scopeInt: String = null
        var optionalInt: Boolean = false
        var optionalIntDefined: Boolean = false;
        
        val exclusionsThis = new LinkedHashSet[Artifact]

        def exclusions = {
            val result = new LinkedHashSet[Artifact]
            val dependency = findTemplateDependency(artifact, template)
            if (dependency != null) {
                result ++= dependency.exclusions
            }
            result ++=exclusionsThis
            result
        }
        
        def addExclusion(artifact: Artifact) = exclusionsThis += artifact
        def updateScope(scope: String) = this.scopeInt = scope
        def updateOptional(optional: Boolean) = {
            this.optionalInt = optional
            this.optionalIntDefined = true
        }

        private var v: Version = null
        private var t: String = null
        private var c: String = null
        
        var groupId: String = null
        var artifactId: String = null
        
        def update(groupId: String, artifactId: String, version: String, typ: String, classifier: String): Unit = 
            update(groupId, artifactId, MavenVersion(version), typ, classifier)
            
        def update(groupId: String, artifactId: String, version: Version, typ: String, classifier: String): Unit = {
            this.groupId = groupId
            this.artifactId = artifactId
            this.v = version
            this.t = typ
            this.c = classifier
        }
        
        def update(artifact: Artifact) = {
                groupId = artifact.groupId
            artifactId = artifact.artifactId
        }
        
        def version: Version = {
            if (v != null) return v
            val dependency = findTemplateDependency(this, template)
            if (dependency != null) {
                return dependency.version
            }
            throw new POMException("Incomplete POM - missing version for " + this.toStringThis)
        }
        
        def typ: String = {
            if (t != null) return t
            val dependency = findTemplateDependency(this, template)
            if (dependency != null) {
                return dependency.typ
            }
            
            null
        }

        def classifier: String = {
            if (c != null) return c
            val dependency = findTemplateDependency(this, template)
            if (dependency != null) {
                return dependency.classifier
            }

            null
        }

        def scope: String = {
            if (scopeInt != null) return scopeInt
            val dependency = findTemplateDependency(this, template)
            if (dependency != null) {
                return dependency.scope
            }

            null
        }

        def optional: Boolean = {
            if (optionalIntDefined) return optionalInt
            val dependency = findTemplateDependency(this, template)
            if (dependency != null) {
                return dependency.optional
            }

            false
        }
        
        def toStringThis = {
            val res = new StringBuilder
            res.append("POMDependencyArtifact[")
            res.append(groupId)
            res.append(":")
            res.append(artifactId)
            if (v != null) {
                res.append(":")
                res.append(v)
            }
            if (t != null) {
                if (v == null) { res.append(":") }
                res.append(":")
                res.append(t)
            }
            if (classifier != null) {
                if (v == null) { res.append(":") }
                if (t == null) { res.append(":") }
                res.append(":")
                res.append(c)
            }
            res.append("]")
            res.toString
        }
    }

    /* Public methods */
    
    def parentArtifact: Artifact = parentArtifactInt

    def artifact = artifactInt
    
    def name = nameInt
    
    def description = descriptionInt
    
    def dependencies: Traversable[Dependency] = {
        if (!parsedDependencies) {
            parseDependencies
        }
        deps.values
    }
    
    def repositories: Traversable[Repository] = {
        if (!parsedRepositories) {
            parseRepositories
        }
        repositoriesInt.values
    }
    
    def templateDependencies: Traversable[Dependency] = {
        if (!parsedTemplateDependencies) {
            parseTemplateDependencies
        }
        templateDeps.values
    }
    
    def findTemplateDependency(artifact: Artifact, fromTemplate: Boolean): Dependency = {
        if (!parsedTemplateDependencies) {
            parseTemplateDependencies
        }
        var dependency = if (!fromTemplate) templateDeps.getOrElse(Artifact.baseArtifact(artifact), null) else null
        if (dependency == null) {
            if (parentPOM != null) {
                dependency = parentPOM.findTemplateDependency(artifact, false)
            }
        }
        dependency
    }
    
    def addDependency(artifact: Artifact): Dependency = { 
        val dependency = new InternalDependency(false)
        dependency.update(artifact)
        deps.put(Artifact.baseArtifact(dependency), dependency)
        dependency
    }

    def addTemplateDependency(artifact: Artifact): Dependency = { 
        val dependency = new InternalDependency(true)
        dependency.update(artifact)
        templateDeps.put(Artifact.baseArtifact(dependency), dependency)
        dependency
    }

}



trait Dependency extends Artifact {
    
    def exclusions: Set[Artifact]

    def scope: String
    def optional: Boolean

    def isPOM = "pom".equals(typ)
    
    override def toString = {
        val res = new StringBuilder
        res.append("Dependency[")
        res.append(super.toString)
        res.append(", scope=").append(scope)
        res.append(", optional=").append(optional)
        res.append("]")
        res.toString
    }
}

class POMException(msg: String) extends RuntimeException(msg)

