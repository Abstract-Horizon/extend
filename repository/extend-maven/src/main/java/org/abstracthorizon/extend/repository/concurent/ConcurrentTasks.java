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
 * This interface describes way concurrent tasks are handled.
 * 
 * @author Daniel Sendula
 */
public interface ConcurrentTasks<Result, Definitions> {

    /**
     * Creates new task group.
     * 
     * @return new task group
     */
    TaskGroup<Result, Definitions> newTaskGroup();
    
    /**
     * Releases group.
     * 
     * @param group group
     */
    void releaseGroup(TaskGroup<Result, Definitions> group);
    
    /**
     * Returns all pending tasks.
     * 
     * @return all pending tasks
     */
    Map<Definitions, Task<Result, Definitions>> getPending();
    
    /**
     * Submits new task to be executed in the background.
     * 
     * @param inGroup group task is to be executed in.
     * 
     * @param definitions definitions
     * 
     * @return newly created task
     */
    Task<Result, Definitions> submitNewTask(TaskGroup<Result, Definitions> inGroup, Definitions definitions);
    
    /**
     * Executes new task in current thread.
     * 
     * @param definitions definitions
     * 
     * @return newly created and executed task
     */
    Task<Result, Definitions> executeNewTask(Definitions definitions);
    
    /**
     * Cancels a task.
     * 
     * @param task task to be cancelled
     */
    void cancel(Task<Result, Definitions> task);
}
