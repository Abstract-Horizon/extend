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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 *
 *
 * @author Daniel Sendula
 */
public class TaskGroupImpl<Result, Definitions> implements TaskGroup<Result, Definitions> {

    private ConcurrentTasks<Result, Definitions> parent;
    
    private Map<Definitions, Task<Result, Definitions>> pending = new HashMap<Definitions, Task<Result,Definitions>>();
    
    //private BlockingQueue<Task<Result, Definitions>> queue = new LinkedBlockingQueue<Task<Result,Definitions>>();
    
    private Queue<Task<Result, Definitions>> queue = new LinkedList<Task<Result, Definitions>>();
    
    public TaskGroupImpl(ConcurrentTasks<Result, Definitions> parent) {
        this.parent = parent;
    }
    
    public Map<Definitions, Task<Result, Definitions>> getPending() {
        return pending;
    }

    public synchronized Task<Result, Definitions> takeFinished() throws InterruptedException {
        if (queue.size() == 0) {
            wait();
        }
        return queue.poll();
    }

    public synchronized boolean hasMore() {
        return !queue.isEmpty() || !pending.isEmpty();
    }

    public synchronized void cancelAll() {
        // Since recusively we will call 'finish()' and that method will affect pending we need to make copy and 
        // iterate over it.
        ArrayList<Task<Result, Definitions>> toBeCancelled = new ArrayList<Task<Result,Definitions>>(pending.values());
        for (Task<Result, Definitions> task : toBeCancelled) {
            parent.cancel(task);
        }
    }
    
    protected synchronized void finish(Task<Result, Definitions> task) {
        pending.remove(task.getDefinitions());
        queue.add(task);
        notifyAll();
    }
}
