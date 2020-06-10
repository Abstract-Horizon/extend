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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


/**
 * Simple keystore class is a wrapper around keystore that manages key managers and trust manager and checks if file keystore is stored in changed.
 *
 * @author Daniel Sendula
 */
public class SmartKeystore {
    
    private File file;
    private long lastmodified;
    private KeyStore keystore;
    private char[] keystorePassword;
    private KeyManager[] keyManagers;
    private TrustManager[] trustManagers;
    
    public SmartKeystore(String fileName, String keystorePassword, String keystoreType) throws KeyStoreException {
        this(new File(fileName), keystorePassword, keystoreType);
    }
    
    public SmartKeystore(File file, String keystorePassword, String keystoreType) throws KeyStoreException {
        this.file = file;
        lastmodified = file.lastModified();
        keystore = KeyStore.getInstance(keystoreType);
        this.keystorePassword = keystorePassword.toCharArray();
    }
    
    public File getFile() {
        return file;
    }
    
    public void load() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        try {
            Enumeration<String> enm = keystore.aliases();
            while (enm.hasMoreElements()) {
                String alias = enm.nextElement();
                keystore.deleteEntry(alias);
            }
        } catch (KeyStoreException e) {
            if (!"Uninitialized keystore".equals(e.getMessage())) {
                throw e;
            }
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        keystore.load(fileInputStream, keystorePassword);
        lastmodified = file.lastModified();

        KeyManagerFactory keystoreManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keystoreManagerFactory.init(keystore, getPassPhrase());
        KeyManager[] keyManagers = keystoreManagerFactory.getKeyManagers();
        if (this.keyManagers == null) {
            this.keyManagers = keyManagers;
        } else {
            for (int i = 0; i < keyManagers.length; i++) {
                this.keyManagers[i] = keyManagers[i];
            }
        }
    
        TrustManagerFactory truststoreManagerFactory = TrustManagerFactory.getInstance("SunX509");
        truststoreManagerFactory.init(keystore);
        TrustManager[] trustManagers = truststoreManagerFactory.getTrustManagers();
        if (this.trustManagers == null) {
            this.trustManagers = trustManagers;
        } else {
            for (int i = 0; i < trustManagers.length; i++) {
                this.trustManagers[i] = trustManagers[i];
            }
        }
    
    }
    
    public boolean isChanged() {
        return file.lastModified() != lastmodified;
    }
    
    public KeyStore getKeyStore() {
        return keystore;
    }
    
    public char[] getPassPhrase() {
        return keystorePassword;
    }
    
    public KeyManager[] getKeyManagers() {
        return this.keyManagers;
    }
    
    public TrustManager[] getTrustManagers() {
        return this.trustManagers;
    }
}
