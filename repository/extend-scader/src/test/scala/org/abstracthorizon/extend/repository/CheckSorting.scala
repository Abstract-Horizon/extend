package org.abstracthorizon.extend.repository

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.util.Sorting


class CheckSorting {
    
}

object CheckSorting {

    def main(args: Array[String]) = {

        val array = new ArrayBuffer[String]
        
        array += "two"
        array += "one"
        array += "three"
            
        def lt(l: String, g: String) = if (l.equals("one")) {
                true
            } else if (l.equals("two")) {
                if (g.equals("three")) {
                    true
                } else {
                    false
                }
            } else {
                false
            }

            
        val r = array.sortWith(lt)
                
        val q = array.sortWith({ (l: String, g: String) =>  
            if (l.equals("one")) {
                true
            } else if (l.equals("two")) {
                if (g.equals("three")) {
                    true
                } else {
                    false
                }
            } else {
                false
            }
        })
        
        println(array)
        println(r)
        
        println(r == array)
        
        println(array)
        val x = Sorting.stableSort(array, { (l: String, g: String) =>  
            println("   l: " + l + " g:" + g)
            if (l.equals("one")) {
                true
            } else if (l.equals("two")) {
                if (g.equals("three")) {
                    true
                } else {
                    false
                }
            } else {
                false
            }
        })
        
        println(array)
        println(x)
        println(x == array)
        
    }
    
}