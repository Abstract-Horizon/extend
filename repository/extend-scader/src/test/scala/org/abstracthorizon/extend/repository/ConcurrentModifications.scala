package org.abstracthorizon.extend.repository

import scala.collection.mutable.ArrayBuffer

object ConcurrentModifications {
    
    
    def main(args: Array[String]) = {
    
        val list = new ArrayBuffer[Int]
        list += 1
        list += 2
        list += 3
        list += 4
        list += 5
        list += 6
        list += 7
        list += 8
        list += 9
        list += 10
        
        var i = 0
        
        for (x <- list) {
            i = i + 1
            
            if (i == 2) {
                list -= x
                println(" - removed " + x)
                i = 0
            } else {
                println("   kept    " + x)
            }
        }
        println("--------------------------------------------------------------------")
        for (x <- list) {
                println("   have    " + x)
        }    
        println("--------------------------------------------------------------------")
        i = 0
        for (x <- list) {
            i = i + 1
            
            if (i == 2) {
                list -= x
                println(" - removed " + x)
                i = 0
            } else {
                println("   kept    " + x)
            }
        }
    }
}