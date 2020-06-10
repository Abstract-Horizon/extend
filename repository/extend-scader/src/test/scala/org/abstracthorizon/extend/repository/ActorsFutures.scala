package org.abstracthorizon.extend.repository

import scala.actors._

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import java.util.Random


object ActorsFutures {
    
    
    val actorsMap = new HashMap[Future[_], ActorWithFuture]
    val channelsMap = new HashMap[Future[_], InputChannel[_]]
    
    def mainx(args: Array[String]) = {
        
        val actors = new ArrayBuffer[ActorWithFuture]
        for (i <- 0 until 20) {
            val actor = new ActorWithFuture(i)
            actors += actor
            actor.start
        }
     
        for (actor <- actors) {
            val future = actor !! START
            actorsMap.put(future, actor)
            val futureChannel = future.inputChannel
            println(actor.id + " Future channel " + futureChannel)
            // channelsMap.put(future, futureChannel)
        }
        
        val timeout = Futures.alarm(2500)
        val timeoutInputChannel = timeout.inputChannel
        // channelsMap.put(timeout, timeoutInputChannel)


        println("Waiting for results")
        
        Actor.receive {
            case actor ! result => {
                result match {
                    case i: Int => {
                        println("  got: " + result + " (from " + actor + ")")
                    }
                    case _ => {
                        println("  TIMEOUT!" + " (from " + actor + ")")
                    }
                }
                stopAll
            }
        }

        println("Got results")
    }

    def stopAll = {
        for (actor <- actorsMap.values) {
            actor.stop
        }
    }


}

case object START

class ActorWithFuture(val id: Int) extends Actor {

    val randomGenerator = new Random

    var thread: Thread = null
    
    def act() = {
        loop {
            react {
                case _ => {
                    thread = Thread.currentThread
                    val n = randomGenerator.nextInt(1000) + 2000
                    
                    try {
                        Thread.sleep(n)
                    } catch {
                        case e: InterruptedException => {
                            println(id + ": interrupted!")
                            exit()
                        }
                    }
                    
                    reply(id)
                    println(id + ": Finished work")
                    exit()
                }
            }
        }
    }
    
    def stop() = {
        println(id + ": Stopping it.")
        thread.interrupt
    }
    
}