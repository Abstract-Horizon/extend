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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Class with utility methods for URLs.
 *
 * @author Daniel Sendula
 */
public class URLUtils {

    /**
     * Adds given file to the file portion of supplied url
     * @param url url
     * @param file string to be added to the end of file part of given url
     * @return new url
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException 
     */
    public static URL add(URL url, String file) throws MalformedURLException, UnsupportedEncodingException {
        String p = addPaths(url.getFile(), file);
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), p);
    }

    /**
     * Adds given file to the file portion of supplied url
     * @param uri URI
     * @param file string to be added to the end of file part of given url
     * @return new URI
     * @throws MalformedURLException
     */
    public static URI add(URI uri, String file) throws URISyntaxException {
        String p = addPaths(uri.getPath(), file);
        return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), p, uri.getQuery(), uri.getFragment());
    }

    /**
     * Retruns true if a file or directory (or any content) exists at
     * given URL. If URL's protocol is not file then {@link URL#openStream()} method
     * is used.
     * @param url
     * @return <code>true</code> if content exists
     */
    public static boolean exists(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
                return file.exists();
            } catch (UnsupportedEncodingException ignore) {
            }
        } else {
            try {
                InputStream is = url.openStream();
                is.close();
                return true;
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Retruns true if a file or directory (or any content) exists at
     * given URL. If URL's protocol is not file then {@link URL#openStream()} method
     * is used.
     * @param uri
     * @return <code>true</code> if content exists
     */
    public static boolean exists(URI uri) {
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath()); // TODO what if path is null?
            return file.exists();
        } else {
            try {
                URL url = uri.toURL();
                InputStream is = url.openStream();
                is.close();
                return true;
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Checks if given url represents folder or file. Folder is if it ends with &quot;/&quot;
     * @param url url
     * @return <code>true</code> if it is a folder
     */
    public static boolean isFolder(URL url) {
        String file = url.getFile();
        return ((file != null) && (file.endsWith("/")));
    }

    /**
     * Checks if given URI represents folder or file. Folder is if it ends with &quot;/&quot;
     * @param url URI
     * @return <code>true</code> if it is a folder
     */
    public static boolean isFolder(URI url) {
        String file = url.getPath();
        return ((file != null) && (file.endsWith("/")));
    }

    public static String addPaths(String p1, String p2) {
    	if (p1 == null) {
    		return p2;
    	} else if (p2 == null) {
    		return p1;
    	}
    	if (p1.endsWith("/")) {
    		if (p2.equals("/")) {
    			return p1;
    		} else if (p2.startsWith("/")) {
    			return p1 + p2.substring(1);
    		} else {
    			return p1 + p2;
    		}
    	} else if (p2.startsWith("/")) {
    		if (p1.endsWith("/")) {
    			return p1 + p2.substring(1);
    		} else if (p1.equals("/")) {
    			return p2;
    		} else {
    			return p1 + p2;
    		}
    	} else {
    		return p1 + "/" + p2;
    	}
    }
}