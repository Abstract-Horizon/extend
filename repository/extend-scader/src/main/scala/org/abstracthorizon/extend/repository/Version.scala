package org.abstracthorizon.extend.repository

class Version {
    
    def isFinal = true
    
    def matches(version: Version) = false
    
    def toFinal = this
}

class SimpleVersion(protected val versionString: String) extends Version {
    
    override def matches(version: Version) = version match {
        case v: Version => 
            val r = versionString.equals(v.toString)
            r
        case _ => false
    }
    
    override def hashCode = {
        val h = versionString.hashCode
        h
    }
    
    override def toString = versionString
}
