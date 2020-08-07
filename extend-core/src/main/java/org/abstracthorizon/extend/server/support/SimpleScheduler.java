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

/**
 * Simple adapter interface for scheduling jobs
 *
 * @author Daniel Sendula
 */
public interface SimpleScheduler {

    /**
     * Add new task to be scheduled for execution
     * @param runnable
     */
    void schedule(Runnable runnable);

    /**
     * Remove task
     * @param runnable
     */
    void remove(Runnable runnable);

}
