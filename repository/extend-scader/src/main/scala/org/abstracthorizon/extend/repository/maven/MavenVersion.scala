package org.abstracthorizon.extend.repository.maven

import org.abstracthorizon.extend.repository._

object MavenVersion {

    def apply(versionString: String): MavenVersion = if (versionString != null && versionString.trim.size > 0) new MavenVersion(versionString) else null

    def toSnapshot(version: Version): MavenVersion = version match {
            case mv: MavenVersion => if (mv.isFinal) MavenVersion(mv.toString() + "-SNAPSHOT") else mv
            case v: Version => if (v.toString.endsWith("-SNAPSHOT")) MavenVersion(v.toString) else {
                val vs = version.toString
                MavenVersion(vs.substring(0, vs.length - 9))
            }
    }
}

class MavenVersion(s: String) extends SimpleVersion(s: String) {
    
    override def isFinal = !versionString.endsWith("-SNAPSHOT")

    override def toFinal = if (isFinal) this else MavenVersion(versionString.substring(0, versionString.length - 9))


}