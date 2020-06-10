/*
 * Copyright (c) 2005-2011 Creative Sphere Limited.
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
package org.abstracthorizon.extend.client.ssl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.abstracthorizon.extend.Extend;

public class ClientSSL implements Runnable {
    
    private SSLSocketFactory originalSSLSocketFactory;
    private SSLSocketFactory mySSLSocketFactory;
    private SSLContext mySSLContext;
    
    private SmartKeystore truststoreInstance;
    private SmartKeystore keystoreInstance;

    private File keystoreFile;
    private File truststoreFile;
    
    private String keystorePassword;
    private String truststorePassword;
    
    private String keystoreType;
    private String truststoreType;
    
    private int filePoolPeriod = 60000; // 60 * 1000 ms = 1 minute 
    
    private boolean disableHostVerification = true;
    
    private boolean doRun = true;
    private Thread thread;
    
    public SmartKeystore getTruststoreInstance() {
        return truststoreInstance;
    }

    public void setTruststoreInstance(SmartKeystore truststore) {
        this.truststoreInstance = truststore;
    }

    public SmartKeystore getKeystoreInstance() {
        return keystoreInstance;
    }

    public void setKeystoreInstance(SmartKeystore keystore) {
        this.keystoreInstance = keystore;
    }

    public File getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(File keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public File getTruststoreFile() {
        return truststoreFile;
    }

    public void setTruststoreFile(File truststoreFile) {
        this.truststoreFile = truststoreFile;
    }
    
    public void setDisableHostVerification(boolean disableHostVerification) {
        this.disableHostVerification = disableHostVerification;
    }
    
    public boolean isDisableHostVerification() {
        return disableHostVerification;
    }
    
    public void setFilePoolPeriod(int filePoolPeriod) {
        this.filePoolPeriod = filePoolPeriod;
    }
    
    public int getFilePoolPeriod() {
        return filePoolPeriod;
    }
    
    
    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    
    
    public void create() throws Exception {
        
        initDefaults();       
        
        keystoreInstance = new SmartKeystore(getKeystoreFile(), getKeystorePassword(), getKeystoreType());
        truststoreInstance = new SmartKeystore(getTruststoreFile(), getTruststorePassword(), getTruststoreType());
        
        try {
            keystoreInstance.load();
        } catch (Exception e) {
            Extend.info.warn("Failed loading keystore " + truststoreInstance.getFile().getAbsolutePath(), e);
        }
        try {
            truststoreInstance.load();
        } catch (Exception e) {
            Extend.info.warn("Failed loading truststore " + truststoreInstance.getFile().getAbsolutePath(), e);
        }

        setupServerCertificationValidation(keystoreInstance, truststoreInstance);
        if (disableHostVerification) {
            disableHostVerification();
        }
    }
    
    public void start() {
        thread = new Thread(this);
        doRun = true;
        thread.start();
    }
    
    public synchronized void stop() {
        doRun = false;
        notifyAll();
        thread.interrupt();
    }
    
    public void destroy() {
        updoServerCertificationValidationSetup();
    }
    
    
    protected void initDefaults() throws IOException {
        if (getKeystoreFile() == null) {
            File file = findKeystoreFile("keystore");
            setKeystoreFile(file);
            setKeystoreType(findKeystoreType(file));
        }
        if (getKeystoreType() == null) {
            setKeystoreType(findKeystoreType(getKeystoreFile()));
        }
        if (getKeystorePassword() == null) {
            setKeystorePassword(findKeystorePassword(getKeystoreFile()));
        }

        if (getTruststoreFile() == null) {
            File file = findKeystoreFile("truststore");
            setTruststoreFile(file);
            setTruststoreType(findKeystoreType(file));
        }
        if (getTruststoreType() == null) {
            setTruststoreType(findKeystoreType(getTruststoreFile()));
        }
        if (getTruststorePassword() == null) {
            setTruststorePassword(findKeystorePassword(getTruststoreFile()));
        }
    }
    
    protected File findKeystoreFile(String filename) throws IOException {
        File configDir = new File("config");

        File file = new File(filename + ".jks");
        if (!file.exists() && configDir.exists()) {
            file = new File(configDir, filename + ".jks");
        }
        if (!file.exists()) {
            file = new File(filename + ".pkcs12");
        }
        if (!file.exists() && configDir.exists()) {
            file = new File(configDir, filename + ".pkcs12");
        }
        throw new IOException("Can't find " + file + " with .jks or .pkcs12 extensions in current or config dirs.");
    }

    protected String findKeystoreType(File file) throws IOException {
        String filename = file.getName();
        if (filename.endsWith(".jks")) {
            return "jks";
        } else if (filename.endsWith("pkcs12")) {
            return "pkcs12";
        }
        throw new IOException("Cannot determine keystore type; " + file.getAbsolutePath());
    }
    
    protected String findKeystorePassword(File file) throws IOException {
        String filename = file.getName();
        int i = filename.lastIndexOf('.');
        if (i >= 0) {
            filename = filename.substring(0, i);
        }
        filename = filename + ".credentials";
        File passwordFile = new File(file.getParentFile(), filename);
        char[] charBuffer = new char[10240];
        StringBuilder buf = new StringBuilder();
        FileReader fr = new FileReader(passwordFile);
        try {
            int r = fr.read(charBuffer);
            while (r > 0) {
                buf.append(charBuffer, 0, r);
                r = fr.read(charBuffer);
            }
        } finally {
            fr.close();
        }
        return buf.toString();
    }
    
    public void run() {
        while (doRun) {
            boolean trustStoreChanged = truststoreInstance.isChanged();
            boolean keyStoreChanged = keystoreInstance.isChanged();
            if (trustStoreChanged) {
                try {
                    truststoreInstance.load();
                } catch (Exception e) {
                    Extend.info.warn("Failed loading truststore " + truststoreInstance.getFile().getAbsolutePath(), e);
                }
            }
            if (keyStoreChanged) {
                try {
                    keystoreInstance.load();
                } catch (Exception e) {
                    Extend.info.warn("Failed loading keystore " + truststoreInstance.getFile().getAbsolutePath(), e);
                }
            }
            synchronized (this) {
                try {
                    wait(getFilePoolPeriod());
                } catch (InterruptedException ignore) {
                }
            }

        }
    }
    
    private void setupServerCertificationValidation(final SmartKeystore keyStore, final SmartKeystore trustStore) {
        
        if (mySSLContext == null || HttpsURLConnection.getDefaultSSLSocketFactory() != mySSLSocketFactory) {

            try {
                originalSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

                mySSLContext = SSLContext.getInstance("TLS");
                
                KeyManager[] storeKeyManagers = keyStore.getKeyManagers();
                TrustManager[] storeTrustManagers = trustStore.getTrustManagers();
                
                KeyManager[] keyManagers = new KeyManager[storeKeyManagers.length];
                TrustManager[] trustManagers = new TrustManager[storeTrustManagers.length];
 
                for (int i = 0; i < storeTrustManagers.length; i++) { 
                    final int j = i;
                    trustManagers[i] = new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            TrustManager trustManager = trustStore.getTrustManagers()[j];
                            if (trustManager instanceof X509TrustManager) {
                                return ((X509TrustManager)trustManager).getAcceptedIssuers();
                            } else {
                                return null;
                            }
                        }
    
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {
                            TrustManager trustManager = trustStore.getTrustManagers()[j];
                            if (trustManager instanceof X509TrustManager) {
                                ((X509TrustManager)trustManager).checkClientTrusted(certs, authType);
                            }
                        }
    
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {
                            TrustManager trustManager = trustStore.getTrustManagers()[j];
                            if (trustManager instanceof X509TrustManager) {
                                ((X509TrustManager)trustManager).checkServerTrusted(certs, authType);
                            }
                        }
                    };
                }
                
                for (int i = 0; i < storeKeyManagers.length; i++) { 
                    final int j = i;
                
                    keyManagers[i] = new X509KeyManager() {
                    
                        public String[] getServerAliases(String keyType, Principal[] issuers) {
                            KeyManager keyManager = keyStore.getKeyManagers()[j];
                            if (keyManager instanceof X509KeyManager) {
                                return ((X509KeyManager)keyManager).getServerAliases(keyType, issuers);
                            }
                            return null;
                        }
                        
                        public PrivateKey getPrivateKey(String alias) {
                            KeyManager keyManager = keyStore.getKeyManagers()[j];
                            if (keyManager instanceof X509KeyManager) {
                                return ((X509KeyManager)keyManager).getPrivateKey(alias);
                            }
                            return null;
                        }
                        
                        public String[] getClientAliases(String keyType, Principal[] issuers) {
                            KeyManager keyManager = keyStore.getKeyManagers()[j];
                            if (keyManager instanceof X509KeyManager) {
                                return ((X509KeyManager)keyManager).getClientAliases(keyType, issuers);
                            }
                            return null;
                        }
                        
                        public X509Certificate[] getCertificateChain(String alias) {
                            KeyManager keyManager = keyStore.getKeyManagers()[j];
                            if (keyManager instanceof X509KeyManager) {
                                return ((X509KeyManager)keyManager).getCertificateChain(alias);
                            }
                            return null;
                        }
                        
                        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
                            KeyManager keyManager = keyStore.getKeyManagers()[j];
                            if (keyManager instanceof X509KeyManager) {
                                return ((X509KeyManager)keyManager).chooseServerAlias(keyType, issuers, socket);
                            }
                            return null;
                        }
                        
                        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
                            KeyManager keyManager = keyStore.getKeyManagers()[j];
                            if (keyManager instanceof X509KeyManager) {
                                return ((X509KeyManager)keyManager).chooseClientAlias(keyType, issuers, socket);
                            }
                            return null;
                        }
                    };
                }
                
                mySSLContext.init(keyManagers, trustManagers, new java.security.SecureRandom());
                mySSLSocketFactory = mySSLContext.getSocketFactory();
                
                
                HttpsURLConnection.setDefaultSSLSocketFactory(mySSLSocketFactory);
                Extend.info.debug("Successfully set SSL socket factory.");
            } catch (Exception e) {
                Extend.info.info("Exception setting up SSL context and SSL socket factory", e);
            }
        }
    }
    
    private void updoServerCertificationValidationSetup() {
        
        if (originalSSLSocketFactory != null) {
            
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(originalSSLSocketFactory);

                Extend.info.debug("Successfully restored original SSL socket factory.");
            } catch (Exception e) {
                Extend.info.info("Exception restoring original SSL socket factory", e);
            }
        }
    }
    
    public static void disableHostVerification() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception e) {
            Extend.info.info("Exception disabling host verification.", e);
        }
    }
    
}
