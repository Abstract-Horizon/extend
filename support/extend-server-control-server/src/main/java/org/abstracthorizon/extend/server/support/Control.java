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
/**
 *
 */
package org.abstracthorizon.extend.server.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.Module;
import org.abstracthorizon.extend.server.deployment.ModuleId;
import org.abstracthorizon.extend.support.spring.server.SpringBasedServer;

/**
 * Little control class that knows how gracefully to shutdown the spring application server.
 *
 * @author Daniel Sendula
 */
public class Control implements Runnable {

    /** String that must be passed down the socket for control to react */
    public static final String MAGIC = "SHUTDOWN SERVER";

    /** Local port control class is going to wait for signal to shutdown the server. Default is 8019 */
    protected int port = 0;

    /** Thread this control class is going to run under */
    protected Thread thread;

    /** Reference to the server instance */
    protected SpringBasedServer server;

    /** Server socket this control is operating on */
    protected ServerSocket serverSocket;

    /** Address to bind to */
    protected InetAddress address;

    /**
     * Empty constructor
     */
    public Control() {
    }

    /**
     * Starts the thread
     */
    public void start() {
        thread = new Thread(this);
        thread.setName("Extend Control Thread");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Signals the thread that it should stop as soon as possible
     */
    public void stop() {
        thread = null;
    }

    /**
     * Main method
     */
    public void run() {
        try {
            serverSocket = new ServerSocket();
            if ((port <= 0) && (address == null)) {
                serverSocket.bind(null);
            } else {
                InetSocketAddress socketAddress = new InetSocketAddress(address, port);
                serverSocket.bind(socketAddress);
            }
            try {
                writeControlFile();
                serverSocket.setSoTimeout(1000); // Check should it continue to run each second
                while (thread != null) {
                    try {
                        final Socket socket = serverSocket.accept();
                        if (socket != null) {

                            try {
                                socket.setSoTimeout(1000);
                                OutputStream outputStream = socket.getOutputStream();
                                try {
                                    PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream));

                                    InputStream inputStream = socket.getInputStream();
                                    try {
                                        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                                        String line = in.readLine();
                                        while (line != null) {
                                            try {
                                                String result = processLine(line);
                                                out.println(result);
                                                out.flush();
                                            } catch (RuntimeException e) {
                                                Extend.info.error("Problem executing " + line, e);
                                                out.println("ERROR " + e.getMessage());
                                            }
                                            line = in.readLine();
                                        }
                                        Thread.sleep(1000);
                                    } finally {
                                        inputStream.close();
                                    }
                                } finally {
                                    outputStream.close();
                                }
                            } finally {
                                socket.close();
                            }
                        }
                    } catch (SocketTimeoutException ignore) {
                    }
                }
            } finally {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception e) {
            Extend.info.error("Control thread died because of a problem", e);
        }
    }

    /**
     * Returns the port control is going to run on
     * @return the port control is going to run on
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port control is going to run on
     * @param port the port control is going to run on
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns bind address
     * @return bind address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Sets bind address
     * @param address bind address
     */
    public void setAddress(InetAddress address) {
        this.address = address;
    }

    /**
     * Reference to the server instance
     * @return the server instance
     */
    public SpringBasedServer getServer() {
        return server;
    }

    /**
     * Sets reference to the server instance
     * @param server the server instance
     */
    public void setServer(SpringBasedServer server) {
        this.server = server;
    }

    /**
     * Stops the server invoking stop and destory methods.
     */
    public void shutdownServer() {
        server.stop();
        server.destroy();
    }

    /**
     * This method writes control file with
     * server socket's port number
     */
    protected void writeControlFile() {
        int port = serverSocket.getLocalPort();
        try {
            File controlFile = null;
            if ("file".equals(getServer().getHomeLocation().getProtocol())) {
                controlFile = new File(URLDecoder.decode(getServer().getHomeLocation().getFile(), "UTF-8"), "control.port");
            } else {
                controlFile = new File("control.port");
            }
            controlFile.deleteOnExit();

            FileOutputStream out = new FileOutputStream(controlFile);
            try {
                out.write(Integer.toString(port).getBytes());
            } finally {
                out.close();
            }
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException(e2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String processLine(String line) {
        line = line.trim();
        String uline = line.toUpperCase();
        if (uline.equals("SHUTDOWN")) {
            shutdownServer();
            return "OK";
        } else if (uline.startsWith("DEPLOY ")) {
            try {
                String moduleURIString = line.substring(7).trim();
                URI moduleURI = new URI(moduleURIString);
                DeploymentManager deploymentManager = (DeploymentManager)getServer().getServerApplicationContext().getBean("DeploymentManager");
                Module module = deploymentManager.loadAndDeploy(moduleURI);
                if (module != null) {
//                    if (!deploymentManager.getDeployedModules().containsKey(moduleURI)) {
//                        deploymentManager.deploy(moduleURI, module);
//                    }
                    return "OK";
                } else {
                    return "Cannot deploy module " + moduleURIString;
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else if (uline.startsWith("UNDEPLOY ")) {
            String moduleIdString = line.substring(9).trim();
            ModuleId moduleId = ModuleId.parseModuleIdString(moduleIdString);
            DeploymentManager deploymentManager = (DeploymentManager)getServer().getServerApplicationContext().getBean("DeploymentManager");
            Module module = deploymentManager.getDeployedModules().get(moduleId);
            if (module != null) {
                deploymentManager.undeploy(module);
                return "OK";
            } else {
                return "Cannot undeploy module; module not found " + moduleId;
            }
        } else if (uline.startsWith("STOP ")) {
            String moduleIdString = line.substring(5).trim();
            ModuleId moduleId = ModuleId.parseModuleIdString(moduleIdString);
            DeploymentManager deploymentManager = (DeploymentManager)getServer().getServerApplicationContext().getBean("DeploymentManager");
            Module module = deploymentManager.getDeployedModules().get(moduleId);
            if (module != null) {
                try {
                    deploymentManager.stop(module);
                    return "OK";
                } catch (Throwable e) {
                    StringWriter res = new StringWriter();
                    e.printStackTrace(new PrintWriter(res));
                    return "Cannot start module; " + moduleId + "\n" + res.toString();
                }
            } else {
                return "Cannot stop module; module not found " + moduleId;
            }
        } else if (uline.startsWith("START ")) {
            String moduleIdString = line.substring(6).trim();
            ModuleId moduleId = ModuleId.parseModuleIdString(moduleIdString);
            DeploymentManager deploymentManager = (DeploymentManager)getServer().getServerApplicationContext().getBean("DeploymentManager");
            Module module = deploymentManager.getDeployedModules().get(moduleId);
            if (module != null) {
                try {
                    deploymentManager.start(module);
                    return "OK";
                } catch (Throwable e) {
                    StringWriter res = new StringWriter();
                    e.printStackTrace(new PrintWriter(res));
                    return "Cannot start module; " + moduleId + "\n" + res.toString();
                }
            } else {
                return "Cannot start module; module not found " + moduleId;
            }
        }
        return "Unknown command";
    }
}
