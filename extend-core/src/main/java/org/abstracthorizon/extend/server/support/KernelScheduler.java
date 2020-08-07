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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple scheduler adapter used by the server. It schedules tasks each second.
 *
 * @author Daniel Sendula
 */
public class KernelScheduler implements SimpleScheduler {

    /** Executor */
    protected ScheduledThreadPoolExecutor scheduler;

    /**
     * Empty constructor
     */
    public KernelScheduler() {
    }

    /**
     * Schedules client for execution
     * @param client runnable client
     */
    public void schedule(Runnable client) {
        getScheduler().scheduleWithFixedDelay(client, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Removes client from list of clients to be executed each second
     * @param client client to be removed
     */
    public void remove(Runnable client) {
        scheduler.remove(client);
    }

    /**
     * Sets executor
     * @param scheduler executor
     */
    public void setScheduler(ScheduledThreadPoolExecutor scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Returns executor
     * @return executor that is used
     */
    public ScheduledThreadPoolExecutor getScheduler() {
        if (scheduler == null) {
            scheduler = new ScheduledThreadPoolExecutor(1);
        }
        return scheduler;
    }

    /**
     * Stops the executor
     */
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
