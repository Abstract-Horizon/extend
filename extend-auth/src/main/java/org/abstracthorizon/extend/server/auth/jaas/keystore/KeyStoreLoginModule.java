/*
 * Copyright (c) 2006-2007 Creative Sphere Limited.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500PrivateCredential;

import org.abstracthorizon.extend.Extend;

/**
 * This is keystore login module. This login module checks keystore's
 * certificates
 *
 * @author Daniel Sendula
 */
public class KeyStoreLoginModule implements LoginModule {

    /** Subject */
    protected Subject subject;

    /** Callback handler */
    protected CallbackHandler callbackHandler;

    protected Map<String, ?> sharedState;

    protected Map<String, ?> options;

    /** Keystore URL */
    private String keyStoreURL;

    /** Keystore password */
    private char[] keyStorePassword;

    /** User's password */
    private char[] userPassword;

    /** Username */
    protected String username;

    /** Keystore type */
    private String keyStoreType;

    /** Keystore provider */
    private String keyStoreProvider;

    /** Uninitialised state */
    protected static final int UNINITIALIZED = 0;

    /** Initialised state */
    protected static final int INITIALIZED = 1;

    /** User authenticated state */
    protected static final int AUTHENTICATED = 2;

    /** Logged in state */
    protected static final int LOGGED_IN = 3;

    /** Current state defaulted to uninitialised */
    protected int status = UNINITIALIZED;

    /** x500 principal */
    private javax.security.auth.x500.X500Principal principal;

    /** Certificates */
    private Certificate[] fromKeystore;

    /** Public credentials */
    private java.security.cert.CertPath publicCredentials = null;

    /** Private credential */
    private X500PrivateCredential privateCredential;

    /**
     * Default contructor
     */
    public KeyStoreLoginModule() {
    }

    /**
     * Sets keystore password
     * @param password keystore password
     */
    public void setKeystorePassword(String password) {
        this.keyStorePassword = password.toCharArray();
    }

    /**
     * Sets keystore URL
     * @param url keystore URL
     */
    public void setKeystoreURL(String url) {
        this.keyStoreURL = url;
    }

    /**
     * Returns keystore URL
     * @return keystore URL
     */
    public String getKeystoreURL() {
        return keyStoreURL;
    }

    /**
     * Init method
     * @param subject subject
     * @param callbackHandler handler
     * @param sharedState shared state
     * @param options options
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        keyStoreType = (String)options.get("keyStoreType");
        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        keyStoreProvider = (String)options.get("keyStoreProvider");

        String pass = (String)options.get("keyStorePassword");
        if (pass != null) {
            keyStorePassword = pass.toCharArray();
        } else {
            keyStorePassword = new char[0];
        }

        keyStoreURL = (String) options.get("keyStoreURL");

        status = INITIALIZED;
    }

    /**
     * Login method
     * @return <code>true</code> if successful
     * @throws LoginException
     */
    public boolean login() throws LoginException {
        if (status == LOGGED_IN) {
            return true;
        }
        if ((status == INITIALIZED) || (status == AUTHENTICATED)) {
            obtainAuthenticationDetails();
            getKeyStoreInfo();
            status = AUTHENTICATED;
            return true;
        }

        throw new LoginException("The login module is not initialized");
    }

    /**
     * This method obtains username and password from the party that tries to log in
     * @throws LoginException
     */
    private void obtainAuthenticationDetails() throws LoginException {
        TextOutputCallback bannerCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "Please login to keystore");
        NameCallback aliasCallback = new NameCallback("Keystore alias: ");
        PasswordCallback privateKeyPasswordCallback = new PasswordCallback("Password: ", false);
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION, ConfirmationCallback.OK_CANCEL_OPTION,
                ConfirmationCallback.OK);
        try {
            callbackHandler.handle(
                    new Callback[]{
                            bannerCallback,
                            aliasCallback,
                            privateKeyPasswordCallback,
                            confirmationCallback}
                );
        } catch (IOException e) {
            throw new LoginException("Exception while getting keystore alias and password: " + e);
        } catch (UnsupportedCallbackException e) {
            throw new LoginException("Error: " + e.getCallback().toString() + " is not available to retrieve authentication "
                    + " information from the user");
        }

        int confirmationResult = confirmationCallback.getSelectedIndex();
        if (confirmationResult == ConfirmationCallback.CANCEL) {
            throw new LoginException("Login cancelled");
        }

        username = aliasCallback.getName();

        char[] tmpPassword = privateKeyPasswordCallback.getPassword();
        userPassword = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, userPassword, 0, tmpPassword.length);
        for (int i = 0; i < tmpPassword.length; i++)
            tmpPassword[0] = ' ';
        tmpPassword = null;
        privateKeyPasswordCallback.clearPassword();
    }

    /**
     * This method loads keystore and obtains certificates
     * @throws LoginException
     */
    private void getKeyStoreInfo() throws LoginException {
        /* Get KeyStore instance */
        KeyStore keystore;
        try {
            if ((keyStoreProvider == null) || (keyStoreProvider.length() == 0)) {
                keystore = KeyStore.getInstance(keyStoreType);
            } else {
                keystore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
            }

            /* Load KeyStore contents from file */
            Extend.info.debug("Loading keystore from " + keyStoreURL.toString());
            InputStream in = new URL(keyStoreURL).openStream();
            try {
                keystore.load(in, keyStorePassword);
            } finally {
                in.close();
            }
        } catch (KeyStoreException e) {
            throw new LoginException("The keystore type is not available: " + e);
        } catch (NoSuchProviderException e) {
            throw new LoginException("The keystore provider is not available: " + e);
        } catch (MalformedURLException e) {
            throw new LoginException("Malformed keystoreURL option: " + e);
        } catch (GeneralSecurityException e) {
            throw new LoginException(e.getMessage());
        } catch (IOException e) {
            throw new LoginException("IOException: " + e);
        }

        try {
            fromKeystore = keystore.getCertificateChain(username);
            if (fromKeystore == null || fromKeystore.length == 0 || !(fromKeystore[0] instanceof X509Certificate)) {
                throw new FailedLoginException("Unable to find X.509 certificate chain for " + username + " in keystore");
            } else {
                LinkedList<Certificate> certList = new LinkedList<Certificate>();
                for (int i = 0; i < fromKeystore.length; i++) {
                    certList.add(fromKeystore[i]);
                }
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                publicCredentials = certificateFactory.generateCertPath(certList);
            }
        } catch (KeyStoreException e) {
            throw new LoginException(e.getMessage());
        } catch (CertificateException e) {
            throw new LoginException("X.509 Certificate unavailable: " + e);
        }

        try {
            X509Certificate certificate = (X509Certificate)fromKeystore[0];
            principal = new javax.security.auth.x500.X500Principal(certificate.getSubjectDN().getName());
            Key privateKey = keystore.getKey(username, userPassword);
            if (privateKey == null || !(privateKey instanceof PrivateKey)) {
                throw new FailedLoginException("Unable to recover key from keystore");
            }
            privateCredential = new X500PrivateCredential(certificate, (PrivateKey)privateKey, username);
        } catch (KeyStoreException e) {
            throw new LoginException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new LoginException("No such algorithm: " + e);
        } catch (UnrecoverableKeyException e) {
            throw new FailedLoginException("Unable to recover key from keystore: " + e);
        }
    }

    /**
     * Performs commit
     * @return <code>true</code> if successful
     * @throws LoginException
     */
    public boolean commit() throws LoginException {
        if (status == LOGGED_IN) {
            return true;
        }
        if (status == AUTHENTICATED) {
            if (subject.isReadOnly()) {
                logoutImpl();
                throw new LoginException("Subject is set readonly");
            } else {
                subject.getPrincipals().add(principal);
                subject.getPublicCredentials().add(publicCredentials);
                subject.getPrivateCredentials().add(privateCredential);
                status = LOGGED_IN;
                return true;
            }
        }
        if (status == INITIALIZED) {
            logoutImpl();
            throw new LoginException("Authentication failed");
        }

        throw new LoginException("The login module is not initialized");
    }

    /**
     * Aborts login
     * @return <code>true</code> if successful
     */
    public boolean abort() throws LoginException {
        if ((status == AUTHENTICATED) || (status == LOGGED_IN)) {
            logoutImpl();
            return true;
        }

        return false;
    }

    /**
     * Logs out
     * @return <code>true</code> if successful
     */
    public boolean logout() throws LoginException {
        if (status == LOGGED_IN) {
            logoutImpl();
            return true;
        }

        return false;
    }

    /**
     * Internal log out method
     * @throws LoginException
     */
    private void logoutImpl() throws LoginException {
        for (int i = 0; i < userPassword.length; i++) {
            userPassword[i] = '\0';
        }
        userPassword = null;

        if (subject.isReadOnly()) {
            principal = null;
            publicCredentials = null;
            status = INITIALIZED;

            Iterator<Object> it = subject.getPrivateCredentials().iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (privateCredential.equals(obj)) {
                    privateCredential = null;
                    try {
                        ((Destroyable) obj).destroy();
                        break;
                    } catch (DestroyFailedException dfe) {
                        throw new LoginException("Unable to destroy private credential, " + obj.getClass().getName() + ": " + dfe.getMessage());
                    }
                }
            }

            throw new LoginException("Unable to remove Principal (X500Principal) and public credential from read-only Subject");
        }
        if (principal != null) {
            subject.getPrincipals().remove(principal);
            principal = null;
        }
        if (publicCredentials != null) {
            subject.getPublicCredentials().remove(publicCredentials);
            publicCredentials = null;
        }
        if (privateCredential != null) {
            subject.getPrivateCredentials().remove(privateCredential);
            privateCredential = null;
        }
        status = INITIALIZED;
    }
}
