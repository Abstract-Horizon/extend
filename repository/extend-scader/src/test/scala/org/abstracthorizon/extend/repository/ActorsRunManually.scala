package org.abstracthorizon.extend.repository

import scala.actors.Actor
import scala.actors.Future


object ActorsRunManually {
    
    import scala.collection.mutable.ArrayBuffer
    
    def main(args: Array[String]) = {
        val now = System.currentTimeMillis
        println("Starting...")
        
        val responses = new ArrayBuffer[Future[Any]]
        // val actors = new Array[TestActor](2)
        for (i <- 0 until 100) {
            val actor = new LTestActor
            actor.start
            val art = Artifact("org.ah.teststuff", "test-artifact", new SimpleVersion(i.toString))
            responses += actor !! new LTestDownload(art)
        }
        
        println("Waiting for responses")
        while (responses.size > 0) {
            for (future <- responses) {
                if (future.isSet) {
                    println("Received " + future())
                    responses -= future
                }
            }
        }
        
        println("Finished! It lasted " +  (System.currentTimeMillis - now) + "ms")
        System.exit(0)
    }
}

case class LTestDownload(artifact: Artifact)

object LTestActor {
    import java.util.concurrent.Executors
    val pool = Executors.newFixedThreadPool(3)
    var responseBoolean = true
}

class LTestActor extends Actor {
    import scala.actors.SchedulerAdapter
    import java.util.Random

    val randomGenerator = new Random
    
    start
    
    override def scheduler = new SchedulerAdapter {
        def execute(block: => Unit) =
            LTestActor.pool.execute(new Runnable {
            def run() { block }
        })
    }    
    
    def act = {
        loop {
            react {
                case LTestDownload(a) => {
                    val random = randomGenerator.nextInt(1000) + 100
                    // println("Got download of " + a)
                    LTestActor.responseBoolean != LTestActor.responseBoolean
                    val response = LTestActor.responseBoolean + " for " + a.toString
                    Thread.sleep(random)
                    reply(response)
                    // println("Replied with " + response)
                }
                case _ => {
                    println("Got something but not sure what it is!")
                }
            }
        }
    }

}
