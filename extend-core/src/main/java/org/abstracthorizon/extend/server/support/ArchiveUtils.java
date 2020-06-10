package org.abstracthorizon.extend.server.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.abstracthorizon.extend.Extend;

public class ArchiveUtils {

    public static final int DEFAULT_BUFFER_SIZE = 10240;
    
    /**
     * Downloads the archive
     * @param url url to download archive from
     * @param file result file
     */
    public static void downloadArchive(URL url, File file) {
        downloadArchive(url, file, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Downloads the archive
     * @param url url to download archive from
     * @param file result file
     */
    public static void downloadArchive(URL url, File file, int bufferSize) {
        if (Extend.transport.isDebugEnabled()) {
            Extend.transport.debug("Downloading archive " + url + " to " + file.getAbsolutePath());
        }
        try {
            InputStream is = url.openStream();
            try {
                OutputStream os = new FileOutputStream(file);
                try {
                    copy(is, os, bufferSize);
                } finally {
                    os.close();
                }

            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unpacks the archive to run
     * @param from archive file
     * @param to result directory
     */
    public static void unpackArchive(File from, File to) {
        unpackArchive(from, to, DEFAULT_BUFFER_SIZE, false);
    }
    
    /**
     * Unpacks the archive to run
     * @param from archive file
     * @param to result directory
     */
    public static void unpackArchive(File from, File to, boolean omitTopDirName) {
        unpackArchive(from, to, DEFAULT_BUFFER_SIZE, omitTopDirName);
    }
    
    /**
     * Unpacks the archive to run
     * @param from archive file
     * @param to result directory
     */
    public static void unpackArchive(File from, File to, int bufferSize) {
        unpackArchive(from, to, bufferSize, false);
    }
    
    /**
     * Unpacks the archive to run
     * @param from archive file
     * @param to result directory
     */
    public static void unpackArchive(File from, File to, int bufferSize, boolean omitTopDirName) {
        if (Extend.info.isDebugEnabled()) {
            Extend.info.debug("Unpacking archive " + from.getAbsolutePath() + " to " + to.getAbsolutePath());
        }
        try {
            if (!to.exists()) {
                if (!to.mkdirs()) {
                    throw new RuntimeException(new IOException("Cannot create directories: " + to.getAbsolutePath()));
                }
            }
            JarFile jarFile = new JarFile(from);
            try {
                boolean firstEntry = true;
                Enumeration<JarEntry> enumerator = jarFile.entries();
                while (enumerator.hasMoreElements()) {
                    JarEntry entry = enumerator.nextElement();
                    String entryName = entry.getName();
                    if (firstEntry) {
                        String toName = to.getName();
                        if (entryName.endsWith("/")) {
                            String entryNameToCompare = entryName.substring(0, entryName.length() - 1);
                            if (toName.equals(entryNameToCompare)) {
                                // to = to.getParentFile();
                                omitTopDirName = true;
                            }
                        }
                        firstEntry = false;
                    }
                    if (omitTopDirName && !entryName.equals("/")) {
                        int i = entryName.indexOf('/');
                        if (i > 0) {
                            entryName = entryName.substring(i + 1);
                        }
                    }
                    if (entryName.length() > 0) {
                        File resultFile = new File(to, entryName);
                        if (entry.isDirectory()) {
                            resultFile.mkdirs();
                        } else {
                            InputStream is = jarFile.getInputStream(entry);
                            try {
                                OutputStream os = new FileOutputStream(resultFile);
                                try {
                                    copy(is, os, bufferSize);
                                } finally {
                                    os.close();
                                }
                            } finally {
                                is.close();
                            }
                        }
                    }
                }
           } finally {
               jarFile.close();
           }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        copy(is, os, DEFAULT_BUFFER_SIZE);
    }
    
    public static void copy(InputStream is, OutputStream os, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int r = is.read(buffer);
        while (r > 0) {
            os.write(buffer, 0, r);
            r = is.read(buffer);
        }
    }


}
