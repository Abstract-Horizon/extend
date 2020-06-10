package org.abstracthorizon.extend.repository

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.io.FileNotFoundException

import java.net.URI
import java.net.URL
import java.net.HttpURLConnection

import java.util.Date

import scala.collection.mutable.HashMap

import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

trait Transport {
    def open(path: String): Transport.FileStream
    def copy(file: Transport.FileStream, path: String): Unit
    def delete(path: String): Unit

    Security.addProvider(new BouncyCastleProvider());
}

object Transport {
    
    def ignoreIOError(f: => Unit) = {
        try {
            f
        } catch {
            case ignore: IOException =>
        }
    }

    abstract class FileStream extends InputStream {
        def size: Long
        def lastModified: Date
    }

    abstract class FileStreamImpl(inputStream: InputStream) extends FileStream {

        override def available = inputStream.available
        override def close = inputStream.close
        
        override def mark(readLimit: Int) = inputStream.mark(readLimit)
        override def markSupported = inputStream.markSupported
        override def read = inputStream.read
        override def read(b: Array[Byte], off: Int, len: Int) = inputStream.read(b, off, len)
        override def reset = inputStream.reset
        override def skip(n: Long) = inputStream.skip(n) 
    }
}

object TransportFactory {
    val urlTransportFactory = new TransportFactory {
        val permanentCache = new HashMap[URI, Transport]
        def transport(uri: URI) = {
            permanentCache.get(uri) match {
                case transport: Transport => transport
                case _ => 
                    val t = uri.getScheme match {
                        case "file" => new FileTransport(new File(uri.getPath))
                        case _ => new URLTransport(uri.toURL)
                    }
                    permanentCache.put(uri, t)
                    t
            }
        }
    }
    val defaultTransportFactory = urlTransportFactory
}

trait TransportFactory {
    def transport(uri: URI): Transport 
}

object URLTransportFactory extends TransportFactory {
    def transport(uri: URI): Transport = if (uri.getPath.endsWith("/")) {
        new URLTransport(uri.toURL)
    } else {
        val u = new URI(uri.getScheme, uri.getUserInfo, uri.getHost, uri.getPort, uri.getPath + "/", uri.getQuery, uri.getFragment)
        new URLTransport(u.toURL)
    }
}

abstract class AbstractURLTransport extends Transport with Log {

    def resourceURL(path: String): URL
    
    def open(path: String): Transport.FileStream = {
        val url = resourceURL(path)
        try {
            transport_debug("Opening stream to " + url)
            val urlConnection = url.openConnection
            val sizeValue =  urlConnection.getContentLength
            val lastModifiedValue = if (urlConnection.getLastModified >= 0) new Date(urlConnection.getLastModified) else  null
                
            new Transport.FileStreamImpl(urlConnection.getInputStream) {
                def size = sizeValue
                def lastModified = lastModifiedValue
            }
        } catch {
            case t: Throwable => {
                transport_debug("Failed to open stream to " + url)
                throw t
            }
        }
    }
    
    def copy(inputStream: Transport.FileStream, path: String) = new UnsupportedOperationException

    def delete(path: String) = new UnsupportedOperationException

}

class URLTransport(repositoryURL: URL) extends AbstractURLTransport {
    def resourceURL(path: String): URL = repositoryURL.toURI.resolve(path).toURL
}

class FileTransport(repository: File) extends Transport {

    def open(path: String): Transport.FileStream = {
        val file = new java.io.File(repository, path)
        if (!file.exists) throw new FileNotFoundException(file.getAbsolutePath)

        new Transport.FileStreamImpl(new FileInputStream(file)) {
            def size = file.length
            def lastModified = new Date(file.lastModified)
        }
    }
    
    def copy(inputStream: Transport.FileStream, path: String) = {
        val file = new java.io.File(repository, path)
        
        val dir = file.getParentFile
        if (!dir.exists) {
            if (!dir.mkdirs) {
                throw new IOException("Cannot create dir " + dir.getAbsolutePath)
            }
        }
        
        val outputStream = new FileOutputStream(file)
        try {
            val buffer = new Array[Byte](10240)
            var r = inputStream.read(buffer)
            while ((r > 0) && !Thread.currentThread.isInterrupted) {
                outputStream.write(buffer, 0, r)
                r = inputStream.read(buffer)
            }
            if (Thread.currentThread.isInterrupted) {
                throw new InterruptedException
            }
        } catch {
            case t: Throwable => 
                Transport.ignoreIOError(outputStream.close)
                file.delete
                throw t
        } finally {
            Transport.ignoreIOError(outputStream.close)
        }
    }

    def delete(path: String) = {
        val file = new java.io.File(repository, path)
        if (!file.delete) {
            throw new IOException("Cannot delete " + path)
        }
    }
}

class DigestStream(stream: Transport.FileStream) extends Transport.FileStream {
    
    var sha1HashValue: String = null

    def sha1Hash = sha1HashValue

    def size = stream.size
    def lastModified = stream.lastModified

    val hash = MessageDigest.getInstance("SHA1");
    val digestStream = new DigestInputStream(stream, hash)
    var closed = false
    
    protected def updateDigest() {
        val sha1HashBytes = digestStream.getMessageDigest().digest() 
        val res = new StringBuilder();
        for (b <- sha1HashBytes) {
            var i = b.toInt;
            if (i < 0) {
                i = 256 + i;
            }
            val s = Integer.toHexString(i);
            if (s.length() != 2) {
                res.append('0');
            }
            res.append(s);
        }
        sha1HashValue = res.toString
    }
    
    override def available = digestStream.available
    override def close = {
        if (!closed) {
            digestStream.close
            updateDigest
            closed = true
        }
    }
    
    override def mark(readLimit: Int) = digestStream.mark(readLimit)
    override def markSupported = digestStream.markSupported
    override def read = digestStream.read
    override def read(b: Array[Byte], off: Int, len: Int) = digestStream.read(b, off, len)
    override def reset = digestStream.reset
    override def skip(n: Long) = digestStream.skip(n) 
            
}
