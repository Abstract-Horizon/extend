/*
 * Copyright (c) 2009 Creative Sphere Limited.
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
package org.abstracthorizon.extend.repository.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.concurent.AbstractTask;

/**
 * Task that handles downloads.
 *
 * @author Daniel Sendula
 */
public class DownloadTask extends AbstractTask<File, DownloadTaskDefinitions> {

    private URLConnection connection;
    private InputStream inputStream;
    private File tempFile;
    
    public DownloadTask(DownloadTasks parent, DownloadTaskDefinitions definitions, URLConnection connection) {
        super(parent, definitions);
        this.connection = connection;
    }
    
    public void execute() {
        if (connection == null) {
            return;
        }
        
        try {
            if (Extend.transport.isInfoEnabled()) { Extend.transport.info(connection.getURL() + ": downloading to " + getDefinitions().getDestinationFile().getAbsolutePath()); }

            inputStream = connection.getInputStream();

            
            tempFile = File.createTempFile("extend-download", ".tmp");
            tempFile.delete();
            tempFile = new File(getDefinitions().getDestinationFile().getParentFile(), tempFile.getName());
            File dir = tempFile.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new RuntimeException(new IOException("Cannot create dir " + dir.getAbsolutePath()));
                }
            }
            

            if (Extend.transport.isDebugEnabled()) { Extend.transport.debug(connection.getURL() + ": got stream for " + getDefinitions().getDestinationFile().getAbsolutePath() + "; temp file=" + tempFile.getAbsolutePath()); }
            if (getDefinitions().isSnapshot()) {
                setResult(continueDownload());
            } else {
                setResult(tempFile);
            }
        } catch (IOException ignore) {
            if (Extend.transport.isDebugEnabled()) { Extend.transport.debug(connection.getURL() + ": cannot be downloaded " + ignore); }
        }
    }

    public File continueDownload() {
        File destinationFile = getDefinitions().getDestinationFile();
        if (Extend.transport.isInfoEnabled()) { Extend.transport.info(connection.getURL() + ": downloading contents to " + destinationFile.getAbsolutePath()); }
        try {
            FileOutputStream fos = new FileOutputStream(tempFile);

            try {
                byte[] buffer = new byte[getDefinitions().getLoader().bufferSize];
                int r = inputStream.read(buffer);
                while (r > 0) {
                    fos.write(buffer, 0, r);
                    r = inputStream.read(buffer);
                }

                if (destinationFile.exists()) {
                    if (!destinationFile.delete()) {
                        Extend.debug.warn("Cannot delete " + destinationFile.getAbsolutePath() + " @ \n" + Utils.stackToString(getDefinitions().getStack()));
                        return null;
                    }
                }
                
                if (!tempFile.renameTo(destinationFile)) {
                    Extend.debug.warn("Cannot rename " + tempFile.getAbsolutePath() + " to " + destinationFile.getAbsolutePath() + " @ \n" + Utils.stackToString(getDefinitions().getStack()));
                } else {
                    if (Extend.transport.isDebugEnabled()) { Extend.transport.debug(connection.getURL() + ": downloaded contents to " + destinationFile.getAbsolutePath()); }
                    return destinationFile;
                }
            } catch (IOException e2) {
                Extend.transport.info("Error downloading " + Utils.stackToString(getDefinitions().getStack()), e2);
            } finally {
                if (tempFile.exists()) {
                    if (Extend.debug.isDebugEnabled()) { Extend.debug.debug(connection.getURL() + ": deleting temp file " + tempFile.getAbsolutePath()); }
                    tempFile.delete();
                }
                try {
                    fos.close();
                } catch (IOException ignore) {
                }
            }
        } catch (IOException e) {
            Extend.info.warn("Error creating temp file ", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignore) {
            }
        }
        return null;
    }
    
    public void finish() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ignore) {
        }
    }


}

