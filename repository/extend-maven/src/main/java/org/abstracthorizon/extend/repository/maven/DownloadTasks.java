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
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.Executor;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.concurent.AbstractConcurrentTasks;
import org.abstracthorizon.extend.repository.concurent.AbstractTask;

/**
 * Manager of download tasks. 
 *
 * @author Daniel Sendula
 */
public class DownloadTasks extends AbstractConcurrentTasks<File, DownloadTaskDefinitions>{

    protected ParallelRepositoryModuleLoader loader;
    
    public DownloadTasks(ParallelRepositoryModuleLoader loader, Executor executor) {
        super(executor);
        this.loader = loader;
    }

    protected DownloadTask createNewTask(DownloadTaskDefinitions definitions) {
        URLConnection urlConnection = null;
        Long when = null;
        
        URL url = definitions.getURL();
        if (url != null) {
            when = loader.getFailedURLs().get(url);
        
            if ((when == null) || ((System.currentTimeMillis() - when.longValue()) > loader.getFailedTimeout())) {
                
                try {
                    urlConnection = definitions.getURL().openConnection();
                } catch (IOException e) {
                    Extend.transport.warn("Exception while trying to open URL connection", e);
                }

                urlConnection.setConnectTimeout(loader.getConnectTimeout());
                urlConnection.setReadTimeout(loader.getReadTimeout());

            }
        }
            
        DownloadTask task = new DownloadTask(this, definitions, urlConnection);
        return task;
    }

    protected synchronized void finish(AbstractTask<File, DownloadTaskDefinitions> task) {
        super.finish(task);
        Map<URL, Long> whereToLog;
        if ((task.getResult() == null) && (task.getDefinitions().getURL() != null)) {
            whereToLog = loader.getFailedURLs();
        } else {
            whereToLog = loader.getSucceddedURLs();
        }
        whereToLog.put(task.getDefinitions().getURL(), System.currentTimeMillis());
    }
}
