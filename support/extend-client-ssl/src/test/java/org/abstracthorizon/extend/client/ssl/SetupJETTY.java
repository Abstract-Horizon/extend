package org.abstracthorizon.extend.client.ssl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.SslSocketConnector;

public class SetupJETTY {

    
    public static void main(String[] args) throws Exception {
        Server server = setupJettyServer();
        server.start();
        //setupHTTPConnector(server);
        setupHTTPSConnector(server);
        setupHandler(server);
        
        //server.start();
        while (true) {
            Thread.sleep(1000);
        }
    }
    
    public static Server setupJettyServer() {
        return new Server();
    }
    
    public static void setupHTTPConnector(Server server) throws Exception {
        SocketConnector connector = new SocketConnector();
        connector.setServer(server);
        connector.setPort(8080);
        connector.setMaxIdleTime(30000);
        connector.start();
        server.addConnector(connector);
    }
    
    public static void setupHTTPSConnector(Server server) throws Exception {
        SslSocketConnector connector = new SslSocketConnector();
        connector.setServer(server);
        connector.setPort(8443);
        connector.setMaxIdleTime(30000);
        connector.setKeystore(ClientSSLTest.resourceToFile("server-keystore.jks").getAbsolutePath());
        connector.setPassword("pass12");
        connector.setKeyPassword("pass12");
        connector.setTruststore(ClientSSLTest.resourceToFile("server-truststore.jks").getAbsolutePath());
        connector.setTrustPassword("pass12345");
        connector.setNeedClientAuth(true);
        connector.start();
        server.addConnector(connector);
    }
    
    public static void setupHandler(Server server) throws Exception {
        AbstractHandler handler = new AbstractHandler() {
            
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                response.setContentType("text/plain");
                response.getWriter().println("OK - this works");
                response.flushBuffer();
            }
        };

        server.addHandler(handler);
        handler.start();
    }
  

}
