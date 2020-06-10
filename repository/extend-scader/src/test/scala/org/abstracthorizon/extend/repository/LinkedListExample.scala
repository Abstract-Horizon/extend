package org.abstracthorizon.extend.repository

import scala.collection.mutable.LinkedList
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.LinkedHashSet

object LinkedListExample {
    
    
    def main(args: Array[String]) = {

        implicit def toString(list: Traversable[Int]): String = {
            val res = new StringBuffer
            res.append('[')
            var first = true
            for (i <- list) {
                if (first) first = false else res.append(',')
                res.append(i)
            }
            res.append(']')
            res.toString
        }
        
        var list = new LinkedHashSet[Int]

        list += 1
        list += 2
        list += 3
        list += 4
        
        println("a: " + toString(list))

        list += 5
//        list.+:(0)
//        list.+:(-1)        
        println("b: " + toString(list))
        
        list -= 1
        
        println("c: " + toString(list))
        println("Here")
    }


}