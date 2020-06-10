package org.abstracthorizon.extend.repository

import scala.actors.Actor
import scala.actors.Future
import java.io._
import java.net._

object ActorsRunManually2 {
    
    def main(args: Array[String]) = {
        
        val x = 5;
        
        def method = { a: String => println("This is closure (a=" + a + ") returned in a method and this is " + this) }
        
        println("Starting")
        println("result of method is " + method)
        println("result of method is invoking it is " + method("123"))
        
        val actor = new LTestActor2
        actor.start
        val art = Artifact("org.abstracthorizon.extend", "extend-core", new SimpleVersion("1.2"))
        // val art = Artifact("org.ah.teststuff", "test-artifact", new SimpleVersion("2"))
        val response1 = actor !! new LTestDownload(art)

        Thread.sleep(100)

        val serverSocket = new ServerSocket(12345)
        
        println("Sending hello")
        actor ! "Hello"
        println("Sending invalidate")
        actor.invalidate
        
        println("Waiting for response...")
        println("Got " + response1())
        
        println("Done")
        System.exit(0)
    }
}

class LTestActor2 extends Actor {
    start
    
    var thread: Thread = null
    var connection: HttpURLConnection = null
    
    def act = {
        loop {
            react {
                case LTestDownload(a) => {
                    thread = Thread.currentThread
                    val now = System.currentTimeMillis
                    try {
                        val url = new URL("http://localhost:12345/hello")
                        val connection = url.openConnection
                        connection.setReadTimeout(2000)
                        this.connection = connection.asInstanceOf[HttpURLConnection]
                        val inputStream = connection.getInputStream

                        
                    } catch {
                        case e: InterruptedException  => println("I was interrupted!")
                        case e: IOException => println("Got IO exception - expected it. Hm. " + e.getMessage())
                    }
                    val response = "Got for " + a.toString + " after " + (System.currentTimeMillis - now) + "ms"
                    reply(response)
                }
                case x: Boolean => {
                    println("Sending interrupt...")
                    if (x && thread != null) {
                        thread.interrupt
                        println("Sent interrupt")
                    } else {
                        println("Didn't send interrupt because " + x + " and thread is " + thread)
                    }
                }
                case _ => {
                    println("Got something but not sure what it is!")
                }
            }
        }
    }
    
    def invalidate() {
        println("Sending interrupt...")
        if (thread != null) {
            thread.interrupt
            if (connection != null) {
                connection.disconnect
            }
            println("Sent interrupt")
        } else {
            println("Didn't send interrupt, thread is " + thread)
        }
    }

}
