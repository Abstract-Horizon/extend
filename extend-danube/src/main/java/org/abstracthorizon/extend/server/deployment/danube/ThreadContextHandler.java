/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.extend.server.deployment.danube;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.extend.server.deployment.Module;

import java.io.IOException;

/**
 * This class represents thread context handler. This connection handler
 * sets given module's class loader as context class loader for the thread
 * it operaters under and then invokes further handler. It is helpful since
 * rest of the chain executed after this connection handler have thread
 * with context class loader as of given module's that loaded them.
 *
 * @author Daniel Sendula
 */
public class ThreadContextHandler implements ConnectionHandler {

    /** Module this thread is executing under */
    protected Module module;

    /** Handler to be invoked */
    protected ConnectionHandler handler;

    /** Constructor */
    public ThreadContextHandler(ConnectionHandler handler, Module module) {
        this.module = module;
        this.handler = handler;
    }

    /**
     * This method creates sets context path to be same as context path
     * up to here plus this component's path. Component's path is reset
     * to &quot;<code>/<code>&quot;
     *
     * @param connection socket connection
     * @throws IOException io exception
     */
    public void handleConnection(Connection connection) {
        ClassLoader cl = module.getClassLoader();
        if (cl != null) {
            Thread.currentThread().setContextClassLoader(cl);
        }
        handler.handleConnection(connection);
    }
    
    public Module getModule() {
        return module;
    }
    
    public ConnectionHandler getHandler() {
        return handler;
    }
}
