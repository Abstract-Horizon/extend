package org.abstracthorizon.extend.repository

import org.slf4j.LoggerFactory

object Log {
    val info = LoggerFactory.getLogger("org.abstracthorizon.extend.Info");

    val debug = LoggerFactory.getLogger("org.abstracthorizon.extend.Debug");

    val transport = LoggerFactory.getLogger("org.abstracthorizon.extend.Transport");
   
//    println("Info.info: " + info.isInfoEnabled)
//    println("Info.debug: " + info.isDebugEnabled)
//    println("Debug.info: " + debug.isInfoEnabled)
//    println("Debug.debug: " + debug.isDebugEnabled)
//    println("Transport.info: " + transport.isInfoEnabled)
//    println("Debug.debug: " + transport.isDebugEnabled)
}

trait Log {
    
    def transport(msg: String): Unit = Log.transport.info(msg)
    def transport(msg: String, t: Throwable): Unit = Log.transport.error(msg, t)
    def transport_warning(msg: String, t: Throwable): Unit = Log.transport.warn(msg, t)
    def transport_debug(msg: => String): Unit = if (Log.transport.isDebugEnabled) Log.transport.debug(msg)
    def transport_debug(msg: => String, t: Throwable): Unit = if (Log.transport.isDebugEnabled) Log.transport.debug(msg, t)

    def info(msg: String): Unit = Log.info.info(msg)
    def info_debug(msg: => String) = if (Log.info.isDebugEnabled) Log.info.debug(msg)
    def error(msg: String, t: Throwable): Unit = Log.info.error(msg, t)
    def warning(msg: String, t: Throwable): Unit = Log.info.warn(msg, t)

    def debug(msg: => String): Unit = /*if (Log.debug.isInfoEnabled) */Log.debug.info(msg)
    def debug_error(msg: => String): Unit = Log.debug.error(msg)
    def debug_error(msg: => String, t: Throwable): Unit = Log.debug.error(msg, t)
    def debug_warn(msg: => String): Unit = Log.debug.warn(msg)
    def debug_warn(msg: => String, t: Throwable): Unit = Log.debug.warn(msg, t)
    
}