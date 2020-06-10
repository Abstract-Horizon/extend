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
package org.abstracthorizon.extend.repository.concurent;

import java.util.Map;

/**
 * This interface describes group of tasks. Two main methos are {@link #hasMore()}
 * and {@link #takeFinished()}. With them, code can go through while loop
 * taking finished (cancelled) tasks out and processing results. 
 * 
 * @author Daniel Sendula
 */
public interface TaskGroup<Result, Definitions> {

    /**
     * Returns all pending tasks.
     * 
     * @return pending tasks
     */
    Map<Definitions, Task<Result, Definitions>> getPending();
    
    /**
     * Waits on first task to finish (or to be cancelled) and then returns it.
     * 
     * @return finished task
     * @throws InterruptedException if current thread was interrupted while waiting
     */
    Task<Result, Definitions> takeFinished() throws InterruptedException;
    
    /**
     * Returns if there are more tasks pending or finished.
     * 
     * @return <code>true</code> if there are more tasks pending or finished
     */
    boolean hasMore();
    
    /**
     * Cancels all tasks in this group.
     */
    void cancelAll();
    
}
