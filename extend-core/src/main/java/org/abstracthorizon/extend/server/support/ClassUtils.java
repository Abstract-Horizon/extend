/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
package org.abstracthorizon.extend.server.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class that contains class and url tool methods.
 *
 * @author Daniel Sendula
 */
public class ClassUtils {

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

    /**
     * Creates class loader for given url. If url is directory then it is scanned
     * and all jar files from it added to classpath.
     *
     * @param parent parent class loader or <code>null</code>
     * @param lib url class loader to be created for.
     * @return {@link URLClassLoader} instance
     */
    public static URLClassLoader createClassLoader(ClassLoader parent, URL lib) {
        try {
            Collection<URL> urls = collectFiles(lib);
            urls.add(lib);
            URL[] us = new URL[urls.size()];
            us = urls.toArray(us);
            LocalURLClassLoader classLoader = new LocalURLClassLoader(us, parent);
            return classLoader;
        } catch (IOException e) {
            throw new RuntimeException("Cannot create class loader", e);
        }
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
                    if (/*file.isFile() &&*/ file.getName().endsWith(".jar")) { // TODO is .jar enough?
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
     * Gets home location.
     * @return home location
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException 
     */
    public static URL getHomeLocation(Class<?> cls) throws MalformedURLException, UnsupportedEncodingException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        String className = cls.getName().replace('.', '/') + ".class";
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


    public static class LocalURLClassLoader extends URLClassLoader implements LocalClassLoader {

        public LocalURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public LocalURLClassLoader(URL[] urls) {
            super(urls);
        }

        public LocalURLClassLoader(URL[] urls, ClassLoader parent,
                URLStreamHandlerFactory factory) {
            super(urls, parent, factory);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }

        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = super.loadClass(name, resolve);
            return c;
        }

        public Class<?> loadLocalClass(String name) throws ClassNotFoundException {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                int i = name.lastIndexOf('.');
                if (i != -1) {
                    sm.checkPackageAccess(name.substring(0, i));
                }
            }

            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = findClass(name);
            }
            return c;
        }

        public URL getLocalResource(String name) {
            return findResource(name);
        }

        public Enumeration<URL> getLocalResources(String name) throws IOException {
            return findResources(name);
        }

    }
}
