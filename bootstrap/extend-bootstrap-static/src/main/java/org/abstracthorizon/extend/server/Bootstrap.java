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
package org.abstracthorizon.extend.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <p>Bootstrap class.
 *
 * This class first tries to find home directory of the server:
 *
 * <ul>
 * <li>URL of this class is found</li>
 * <li>then root of class path is taken where this class is in</li>
 * <li>if url is jar then directory jar is in is taken</li>
 * <li>directory's name is checked if it is &quot;bin&quot;, if so - parent is taken</li>
 * </ul>
 * </p>
 * <p>
 *   Then class loader with class path is created with all jar files from lib subdirectory (of the home directory),
 *   and lib directory itself is added to the class path.
 * </p>
 * <p>
 *   After that server directory is determined from arguments (TODO) or if none present from
 *   &quit;server/default&quot; directory of the home directory.
 * </p>
 * <p>
 *   Last part is to load {@link Server} class (using reflection), pass home and server's urls to it
 *   (in constructor) and then to invoke it calling <code>create()</code> and <code>start()</code> methods.
 * </p>
 *
 * @author Daniel Sendula
 */
public class Bootstrap {

    /**
     * Main method
     * @param args arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        URL homeURL = getHomeLocation();
        URL libURL = new URL(homeURL.getProtocol(), homeURL.getHost(), homeURL.getPort(), homeURL.getFile() + "lib/");

        String serverDir = "default";

        int i = 0;
        while (i < args.length) {
            if ("-c".equals(args[i])) {
                i = i + 1;
                if (i >= args.length) {
                    throw new IllegalArgumentException("-c must be followed with name of sub-directory from server directory");
                }
                serverDir = args[i];
            }

            i = i + 1;
        }

        URL serverURL = null;
        File local = new File(URLDecoder.decode(homeURL.getFile(), "UTF-8") + "server/" + serverDir);
        if (local.exists()) {
            serverURL = new URL(homeURL.getProtocol(), homeURL.getHost(), homeURL.getPort(), homeURL.getFile() + "server/" + serverDir + "/");
        } else {
            serverURL = new URL(homeURL.getProtocol(), homeURL.getHost(), homeURL.getPort(), serverDir + "/");
        }

        System.out.println("home = " + homeURL);
        System.out.println("lib = " + libURL);

        URLClassLoader classLoader = createClassLoader(Thread.currentThread().getContextClassLoader(), libURL);

        Class<?> serverClass = classLoader.loadClass("org.abstracthorizon.extend.support.spring.server.SpringBasedServer");

        Constructor<?> constructor = serverClass.getConstructor(new Class[]{URL.class, URL.class});
        Object server = constructor.newInstance(new Object[]{homeURL, serverURL});

        invokeMethod(server, "create", true);
        invokeMethod(server, "start", true);

    }

    /**
     * Gets home location.
     * @return home location
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException 
     */
    public static URL getHomeLocation() throws MalformedURLException, UnsupportedEncodingException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        String className = Bootstrap.class.getName().replace('.', '/') + ".class";
        URL root = contextClassLoader.getResource(className);
        if ("file".equals(root.getProtocol())) {
            String file = URLDecoder.decode(root.getFile(), "UTF-8");
            file = file.substring(0, file.length() - className.length() - 1);
            if (file.endsWith("bin")) {
                file = file.substring(0, file.length() - 3);
            }
            return new File(file).toURI().toURL();
        } else if ("jar".equals(root.getProtocol())) {
            String file = URLDecoder.decode(root.getFile(), "UTF-8");
            //if (!file.startsWith("file")) {
            //    throw new RuntimeException("Cannot handle protocol from where this jar is loaded; " + root);
            //}
            int i = file.lastIndexOf('!');
            file = file.substring(0, i);
            File f = new File(file);
            file = f.getParent();
            if (file.endsWith("bin")) {
                file = file.substring(0, file.length() - 3);
            }
            return new URL(file);
            // return new File(file).toURL();
        } else {
            throw new RuntimeException("Cannot handle protocol from where this class is loaded; " + root);
        }
    }

    /**
     * Creates class loader for given url. If url is directory then it is scanned
     * and all jar files from it added to classpath.
     *
     * @param parent parent class loader or <code>null</code>
     * @param lib url class loader to be created for.
     * @return {@link URLClassLoader} instance
     */
    public static URLClassLoader createClassLoader(ClassLoader parent, URL lib) throws IOException {
        Collection<URL> urls = collectFiles(lib);
        urls.add(lib);
        URL[] us = new URL[urls.size()];
        us = urls.toArray(us);
        URLClassLoader classLoader = new URLClassLoader(us, parent);
        return classLoader;
    }

    /**
     * Collects files from given url.
     * @param url url
     * @return collection of urls
     * @throws IOException
     */
    public static Collection<URL> collectFiles(URL url) throws IOException {
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            return collectFilesFromPath(new File(URLDecoder.decode(url.getFile(), "UTF-8")));
        } else if ("jar".equals(protocol)) {
            JarURLConnection jarURLConnection = (JarURLConnection)url.openConnection();
            return collectFilesFromJar(url, jarURLConnection.getJarFile());
        } else {
            return collectFilesFromURL(url);
        }
    }

    /**
     * Collects files from given path
     * @param path directory
     * @return collection of urls
     * @throws IOException
     */
    public static Collection<URL> collectFilesFromPath(File path) throws IOException {
        ArrayList<URL> urls = new ArrayList<URL>();
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (File file : files) {
                    if (file.isFile()) {
                        urls.add(file.toURI().toURL());
                    }
                }
            }
        }
        return urls;
     }

    /**
     * Collects files from jar file (root of)
     * @param original original url
     * @param jarFile jar file
     * @return collection of urls
     * @throws IOException
     */
    public static Collection<URL> collectFilesFromJar(URL original, JarFile jarFile) throws IOException {
        ArrayList<URL> urls = new ArrayList<URL>();

        String originalFile = original.getFile();
        String prefix = originalFile.substring(originalFile.lastIndexOf('!') + 1);
        if (prefix.endsWith("/")) {
            if (prefix.startsWith("/")) {
                prefix = prefix.substring(1);
            }
            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                String path = entry.getName();
                if (path.startsWith(prefix)) {
                    path = path.substring(prefix.length());
                    if (path.length() > 0) {
                        int i = path.indexOf('/');
                        if ((i < 0) /*|| (i == (path.length() - 1))*/) {
                            URL url = new URL(original.getProtocol(), original.getHost(), original.getPort(), originalFile + path);
                            urls.add(url);
                        }
                    }
                }
            }
        }
        return urls;
     }

    /**
     * Collects files from give (generic) url
     * @param url url
     * @return collection of files
     * @throws IOException
     */
    public static Collection<URL> collectFilesFromURL(URL url) throws IOException {
        ArrayList<URL> urls = new ArrayList<URL>();
        InputStream is = url.openStream();
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            String file = input.readLine();
            while (file != null) {
                if (file.length() > 0) {
                    URL newUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "/" + file);
                    urls.add(newUrl);
                }
                file = input.readLine();
            }
        } finally {
            is.close();
        }
        return urls;
     }

    /**
     * Invokes parametherless method with given name. If <code>mustBePresent</code>
     * is set to <code>true</code> {@link NoSuchMethodException} is propagated wrapped in
     * {@link RuntimeException}.
     *
     * @param object instance on which method is going to be executed
     * @param methodName method name
     * @param mustBePresent if set then method must be present or {@link NoSuchMethodError} is thrown
     * @throws RuntimeException if {@link NoSuchMethodException}, {@link IllegalArgumentException},
     * {@link IllegalAccessException} or {@link InvocationTargetException} is thrown.
     */
    public static void invokeMethod(Object object, String methodName, boolean mustBePresent) {
        try {
            Method method = object.getClass().getMethod(methodName, new Class[]{});
            method.invoke(object, new Object[]{});
        } catch (NoSuchMethodException e) {
            if (mustBePresent) {
                throw new RuntimeException(e);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
