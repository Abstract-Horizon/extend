package org.abstracthorizon.extend.repository.util;

import java.io._
import scala.collection.mutable.ListBuffer

object IOUtils {

    implicit def asString(stream: InputStream): String = {
        val res = new StringBuilder
        try {
            val buffer = new Array[Byte](10240)
            var r = stream.read(buffer)
            while (r > 0) {
                res.append(new String(buffer, 0, r))
                r = stream.read(buffer)
            }
        } finally {
            stream.close
        }
        res.toString
    }
    
    implicit def asString(file: File): String = asString(new FileInputStream(file))
    
    implicit def streamToStringArray(stream: InputStream): Traversable[String] = {
        val bufferedStream = new BufferedReader(new InputStreamReader(stream))
        val res = new ListBuffer[String]
        var s = bufferedStream.readLine
        while (s != null) {
            res += (s);
            s = bufferedStream.readLine
        }
        res
    }
    
    implicit def fileToStringArray(file: File): Traversable[String] = streamToStringArray(new FileInputStream(file))
}

