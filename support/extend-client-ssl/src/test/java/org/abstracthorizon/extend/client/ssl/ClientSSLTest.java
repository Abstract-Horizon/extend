package org.abstracthorizon.extend.client.ssl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;

public class ClientSSLTest {
    
    public static SmartKeystore trustStore;
    public static SmartKeystore keyStore;

    
    private Server server;
    
    public static void main(String[] args) throws Exception {

        ClientSSLTest test = new ClientSSLTest();
        test.setupJetty();
        
        Thread.sleep(3000);
        
        ClientSSL clientSSL = new ClientSSL();
        clientSSL.setKeystoreFile(resourceToFile("client.pkcs12"));
        clientSSL.setKeystorePassword("pass123");
        clientSSL.setKeystoreType("pkcs12");

        clientSSL.setTruststoreFile(resourceToFile("client-truststore.jks"));
        clientSSL.setTruststorePassword("pass1234");
        clientSSL.setTruststoreType("jks");

        
        clientSSL.create();
        clientSSL.start();
        
        boolean succeeded = false;
        while (!succeeded) {
            succeeded = tryLoading("https://127.0.0.1:8443/");
            if (!succeeded) {
                Thread.sleep(1000);
            }
        }
        System.exit(0);
    }
    
    @Before
    public void setup() throws Exception {
        setupJetty();
    }
    
    @After
    public void tearDown() throws Exception {
        tearDownJetty();
    }
    
    @Test
    public void testClientSSL() throws Exception {
        File clientPKCS12 = resourceToFile("client.pkcs12");
        File clientOKPKCS12 = resourceToFile("client-ok.pkcs12");
        File clientBadPKCS12 = resourceToFile("client-bad.pkcs12");

        clientOKPKCS12 = new File(clientOKPKCS12.getParentFile(), clientOKPKCS12.getName());
        
        copyFile(clientBadPKCS12, clientPKCS12);
        
        ClientSSL clientSSL = new ClientSSL();
        clientSSL.setKeystoreFile(resourceToFile("client.pkcs12"));
        clientSSL.setKeystorePassword("pass123");
        clientSSL.setKeystoreType("pkcs12");

        clientSSL.setTruststoreFile(resourceToFile("client-truststore.jks"));
        clientSSL.setTruststorePassword("pass1234");
        clientSSL.setTruststoreType("jks");

        clientSSL.setFilePoolPeriod(500);
        
        clientSSL.create();
        clientSSL.start();

        Thread.sleep(3000);
        
        boolean succeeded = tryLoading("https://127.0.0.1:8443/");

        Assert.assertFalse("Expected to fail" , succeeded);
        
        copyFile(clientOKPKCS12, clientPKCS12);
        
        Thread.sleep(3000);
        
        succeeded = tryLoading("https://127.0.0.1:8443/");

        Assert.assertTrue("Expected to succeed" , succeeded);
        
    }
    
    public static void copyFile(File from, File to) throws IOException {
        byte[] buffer = new byte[10240];
        FileInputStream fis = new FileInputStream(from);
        try {
            FileOutputStream fos = new FileOutputStream(to);
            try {
                int r = fis.read(buffer);
                while (r > 0) {
                    fos.write(buffer, 0, r);
                    r = fis.read(buffer);
                }
            } finally {
                fos.close();
            }
        } finally {
            fis.close();
        }
    }
    
    public static File resourceToFile(String name) {
        URL url = ClientSSLTest.class.getResource("/" + name);
        
        String fileName;
        try {
            fileName = URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return new File(fileName);
    }
    
    
    public void setupJetty() throws Exception {
        server = SetupJETTY.setupJettyServer();
        server.start();

        SetupJETTY.setupHTTPSConnector(server);
        SetupJETTY.setupHandler(server);
    }
    
    public void tearDownJetty() throws Exception {
        server.stop();
        server.destroy();
    }
    
    public static boolean tryLoading(String page) {
        try {
            URL url = new URL(page);
            
            InputStream is = url.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            
            
            String line = in.readLine();
            while (line != null) {
                System.out.println(" > " + line);
                line = in.readLine();
            }
    
            is.close();
            
            return true;
        } catch (Exception ignore) {
            System.out.println("Failed!");
            ignore.printStackTrace(System.out);
        }
        return false;
    }
    
}
