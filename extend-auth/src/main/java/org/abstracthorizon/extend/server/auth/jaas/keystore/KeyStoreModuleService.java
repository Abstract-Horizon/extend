/*
 * Copyright (c) 2006-2020 Creative Sphere Limited.
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
package org.abstracthorizon.extend.server.auth.jaas.keystore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.abstracthorizon.extend.Extend;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;


/**
 * This is keystore login module service. This class sets up keystore login module
 *
 * @author Daniel Sendula
 */
public class KeyStoreModuleService implements ApplicationContextAware {

    /** Keystore URL */
    private String keystore;

    /** Keystore password */
    private String keystorePassword;

    /** Keystore type */
    private String keystoreType = KeyStore.getDefaultType();

    /** Keystore provider */
    private String keystoreProvider = "";

    /** Login context name */
    private String loginContext;

    /** Control flag defaulted to &quot;requrired&quot;*/
    private String controlFlag = "required";

    /** Configuration */
    private Configuration configuration;

    /** Options */
    private Map<String, Object> options = new HashMap<String, Object>();

    /** Context this service was defined in */
    private ApplicationContext context;

    private Resource keystoreResource;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Default constructor
     */
    public KeyStoreModuleService() {
    }

    /**
     * Starts the service - adding keystore login module to system application configuration entry
     * @throws Exception
     */
    public void start() throws Exception {

        AppConfigurationEntry.LoginModuleControlFlag flag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;

        if (AppConfigurationEntry.LoginModuleControlFlag.REQUIRED.toString().indexOf(controlFlag) > 0 ) {
            flag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
        } else if( AppConfigurationEntry.LoginModuleControlFlag.REQUISITE.toString().indexOf(controlFlag) > 0 ) {
            flag = AppConfigurationEntry.LoginModuleControlFlag.REQUISITE;
        } else if( AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT.toString().indexOf(controlFlag) > 0 ) {
            flag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        }else if( AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL.toString().indexOf(controlFlag) > 0 ) {
            flag = AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
        }

        AppConfigurationEntry entry = new AppConfigurationEntry(KeyStoreLoginModule.class.getName(), flag, options);

        AppConfigurationEntry[] list = new AppConfigurationEntry[1];
        list[0] = entry;
        Method method = configuration.getClass().getMethod("setAppConfigurationEntry", new Class[]{String.class, list.getClass()});
        Object[] args = {loginContext, list};
        method.invoke(configuration, args);
        Extend.info.debug("Set up login context '" + loginContext + "'");
    }


    public void stop() throws Exception {
        Method method = configuration.getClass().getMethod("removeAppConfigurationEntry", new Class[]{String.class});
        Object[] args = {loginContext};
        method.invoke(configuration, args);
        Extend.info.debug("Removed login context '" + loginContext + "'");
    }

    /**
     * Lists certificates (users) from the keystore
     * @return users as a string
     * @throws Exception
     */
    public String listUsersString() throws Exception {
        List<String> u = listUsers();
        String[] users = new String[u.size()];
        users = (String[])u.toArray(users);

        StringBuffer res = new StringBuffer();
        res.append("[");
        for (int i=0; i<users.length; i++) {
            if (i > 0) {
                res.append(", ");
            }
            res.append(users[i]);
        }
        res.append("]");
        return res.toString();
    }

    /**
     * Returns a list of all users in the keystore
     * @return list of all users in the keystore
     * @throws Exception
     */
    public List<String> listUsers() throws Exception {
        KeyStore keyStore = loadKeyStore();
        ArrayList<String> users = new ArrayList<String>();
        Enumeration<String> en = keyStore.aliases();
        while (en.hasMoreElements()) {
            users.add(en.nextElement());
        }
        return users;
    }

    /**
     * Changes the password of the user
     * @param user user
     * @param oldPassword old password
     * @param newPassword new password
     * @throws Exception
     */
    public void changePassword(String user, String oldPassword, String newPassword) throws Exception {
        KeyStore keyStore = loadKeyStore();
        Key key = keyStore.getKey(user, oldPassword.toCharArray());

        Certificate[] certs = keyStore.getCertificateChain(user);

        keyStore.setKeyEntry(user, key, newPassword.toCharArray(), certs);
        storeKeyStore(keyStore);
    }

    /**
     * Adds new user to the store
     * @param user user
     * @param passwd password
     * @throws Exception
     */
    public void addUser(String user, String passwd) throws Exception {
        KeyStore keyStore = loadKeyStore();

        String name = "CN="+user+", OU=, O=, L=, ST=, C=";
            //CN=Me, OU=Java Card Development, O=MyFirm, C=UK, ST=MyCity";

        Security.addProvider(new BouncyCastleProvider());

        X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
        kpGen.initialize(1024);
        KeyPair pair = kpGen.generateKeyPair();

        generator.setSerialNumber(BigInteger.valueOf(1));
        generator.setIssuerDN(new X509Principal(name));
        generator.setNotBefore(new Date());
        generator.setNotAfter(new Date(System.currentTimeMillis()+1000*60*60*24*365));
        generator.setSubjectDN(new X509Principal(name));
        generator.setPublicKey(pair.getPublic());
        generator.setSignatureAlgorithm("MD5WithRSAEncryption");

        Certificate cert = generator.generate(pair.getPrivate(), "BC");

        keyStore.setKeyEntry(user, pair.getPrivate(), passwd.toCharArray(), new Certificate[]{cert});
        storeKeyStore(keyStore);
    }

    /**
     * Removes certificate  - user
     * @param user username
     * @throws Exception
     */
    public void removeUser(String user) throws Exception {
        KeyStore keyStore = loadKeyStore();
        keyStore.deleteEntry(user);
        storeKeyStore(keyStore);
    }

    protected KeyStore loadKeyStore() throws KeyStoreException, NoSuchProviderException, MalformedURLException,
        IOException, NoSuchAlgorithmException, CertificateException {

        Extend.info.debug("Loading keystore from " + keystoreResource.toString() + " for login context '" + loginContext + "'");
        InputStream keystoreInputStream = keystoreResource.getInputStream();
        try {
            KeyStore keyStore;
            if ((keystoreProvider == null) || (keystoreProvider.length() == 0)) {
                keyStore = KeyStore.getInstance(keystoreType);
            } else {
                keyStore = KeyStore.getInstance(keystoreType, keystoreProvider);
            }

            /* Load KeyStore contents from file */
            keyStore.load(keystoreInputStream, keystorePassword.toCharArray());
            return keyStore;

        } finally {
            keystoreInputStream.close();
        }
    }

    /**
     * Stores keystore back to provided resource
     * @param keystore keystore to be stored
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws MalformedURLException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    @SuppressWarnings("resource")
	protected void storeKeyStore(KeyStore keystore) throws KeyStoreException, NoSuchProviderException, MalformedURLException,
        IOException, NoSuchAlgorithmException, CertificateException {

        Extend.info.debug("Storing keystore to " + keystoreResource.toString() + " for login context '" + loginContext + "'");
        OutputStream keystoreOutputStream = null;
        try {
            File file = keystoreResource.getFile();
            keystoreOutputStream = new FileOutputStream(file);
        } catch (IOException e) {
            URLConnection connection = keystoreResource.getURL().openConnection();
            connection.setDoOutput(true);
            keystoreOutputStream = connection.getOutputStream();
        }

        try {
            keystore.store(keystoreOutputStream, keystorePassword.toCharArray());
        } finally {
            keystoreOutputStream.close();
        }
    }

    /**
     * Sets keystore password
     * @param password keystore password
     */
    public void setKeyStorePassword(String password) {
        this.keystorePassword = password;
        if (password != null) {
            options.put("keyStorePassword", password);
        } else {
            options.remove("keyStorePassword");
        }
    }

    /**
     * Keystoore location
     * @param keystore keystore location
     * @throws IOException
     */
    public void setKeyStore(String keystore) throws IOException {
        this.keystore = keystore;

        if ((keystore != null) && (context != null)) {
            keystoreResource = context.getResource(keystore);
            options.put("keyStoreURL", keystoreResource.getURL().toString());
        } else {
            keystoreResource = null;
            options.remove("keyStoreURL");
        }
    }

    /**
     * Returns keystore url
     * @return keystore url
     */
    public String getKeyStoreURL() {
        return keystore;
    }

    /**
     * Sets keystore type
     * @param type keystore type
     */
    public void setKeyStoreType(String type) {
        keystoreType = type;
        if (type != null) {
            options.put("keyStoreType", type);
        } else {
            options.remove("keyStoreType");
        }
    }

    /**
     * Returns keystore type
     * @return keystore type
     */
    public String getKeyStoreType() {
        return keystoreType;
    }

    /**
     * Sets keystore provider
     * @param provider keystore provider
     */
    public void setKeyStoreProvider(String provider) {
        this.keystoreProvider = provider;
        if (provider != null) {
            options.put("keyStoreProvider", provider);
        } else {
            options.remove("keyStoreProvider");
        }
    }

    /**
     * Returns keystore provider
     * @return keystore provider
     */
    public String getKeyStoreProvider() {
        return keystoreProvider;
    }

    /**
     * Sets login context name
     * @param loginContext login context name
     */
    public void setLoginContext(String loginContext) {
        this.loginContext = loginContext;
    }

    /**
     * Returns login context name
     * @return login context name
     */
    public String getLoginContext() {
        return loginContext;
    }

    /**
     * Sets control flag
     * @param controlFlag control flag
     */
    public void setControlFlag(String controlFlag) {
        this.controlFlag = controlFlag;
    }

    /**
     * Returns control flag
     * @return control flag
     */
    public String getControlFlag() {
        return controlFlag;
    }

    /**
     * Returns configuration
     * @return configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Sets configuration
     * @param configuration configuration
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Sets application context (module) this service is defined in
     * @param context context
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
        try {
            setKeyStore(keystore);
        } catch (IOException e) {
            throw new BeanInitializationException("Problem setting keystore", e);
        }
    }
}
