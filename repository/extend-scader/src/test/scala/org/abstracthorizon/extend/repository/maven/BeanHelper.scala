package org.abstracthorizon.extend.repository.maven

import scala.collection.Seq
import scala.collection.Traversable

object BeanHelper {

    def readAsString(bean: AnyRef, path: String): String = {
        val r = read(bean, path)
        if (r != null) {
            return r.toString
        }
        return null
    }
    def read(bean: AnyRef, path: String): Any = {
        var current = bean
        var segments = path.split("\\.")
        for (segment <- segments) {
            var s = segment
            var index = -1;
            val start = segment.indexOf("(") 
            if (start > 0) {
                if (segment.charAt(segment.length - 1) != ')') {
                    throw new RuntimeException("Wrong segment: '" + segment)
                }
                s = segment.substring(0, start)
                index = Integer.parseInt(segment.substring(start + 1, segment.length - 1))
            }
            
            try {
                val cls = current.getClass()
                val method = cls.getMethod(s)
                current = method.invoke(current)
                if (index >= 0) {
                    if (current.isInstanceOf[Seq[_]]) {
                        val seq = current.asInstanceOf[Seq[_]]
                        var r = seq(index)
                        if (r.isInstanceOf[AnyRef]) {
                            current = r.asInstanceOf[AnyRef]
                        } else {
                            r
                        }
                    } else if (current.isInstanceOf[Traversable[_]]) {
                        val seq = current.asInstanceOf[Traversable[_]]
                        var r = seq.toSeq(index)
                        if (r.isInstanceOf[AnyRef]) {
                            current = r.asInstanceOf[AnyRef]
                        } else {
                            r
                        }
                    } else {
                        throw new RuntimeException("Cannot select index of " + current)
                    }
                }
            } catch {
                case e: NoSuchMethodException => throw new RuntimeException("Failed to find method '" + s + "' on object: " + current + "; path='" + path + "'", e)
            }
        }
        current
    }
    
}