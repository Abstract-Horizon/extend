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

/**
 * This interface describes a task.
 * 
 * @author Daniel Sendula
 */
public interface Task<Result, Definitions> extends Runnable {

    /**
     * Definitions of the task. This may be input parameters etc.
     * Important thing is to implement definition's {@link Object#equals(Object)} and {@link Object#hashCode()} methods
     * so tasks can be compared based on definitions. 
     * 
     * @return definitions that define this task
     * @see Object#equals(Object)
     * @see Object#hashCode()
     */
    Definitions getDefinitions();
    
    /**
     * This method will return result of computation or <code>null</code>
     * 
     * @return result of computation or <code>null</code>
     */
    Result getResult();

    /**
     * Returns exception (if any) in case task has thrown one or was interrupted.
     * 
     * @return exception or <code>null</code>
     */
    Throwable getException();
    
    /**
     * This method is not be called by client. It is here
     * for {@link ConcurrentTasks} to invoke. Instead of this
     * method use {@link ConcurrentTasks#cancel(Task)}.
     */
    void cancel();
}
