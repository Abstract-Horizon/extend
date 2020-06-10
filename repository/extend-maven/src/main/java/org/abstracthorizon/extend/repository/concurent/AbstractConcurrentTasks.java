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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 *
 *
 * @author Daniel Sendula
 */
public abstract class AbstractConcurrentTasks<Result, Definitions> implements ConcurrentTasks<Result, Definitions> {

    private Executor executor;
    
    private Map<Definitions, Task<Result, Definitions>> tasks = new HashMap<Definitions, Task<Result,Definitions>>();
    
    private Set<TaskGroupImpl<Result, Definitions>> groups = new HashSet<TaskGroupImpl<Result,Definitions>>();
    
    public AbstractConcurrentTasks(Executor executor) {
        this.executor = executor;
    }
    
    public Map<Definitions, Task<Result, Definitions>> getPending() {
        return tasks;
    }

    public synchronized TaskGroup<Result, Definitions> newTaskGroup() {
        TaskGroupImpl<Result, Definitions> group = new TaskGroupImpl<Result, Definitions>(this);
        groups.add(group);
        return group;
    }
    
    public synchronized void releaseGroup(TaskGroup<Result, Definitions> group) {
        groups.remove(group);
        if (group.hasMore()) {
            throw new RuntimeException("Releasing group with more tasks in it!");
        }
    }

    public synchronized Task<Result, Definitions> submitNewTask(TaskGroup<Result, Definitions> inGroup, Definitions definitions) {
        Task<Result, Definitions> task = tasks.get(definitions);
        if (task == null) {
            task = createNewTask(definitions);
            tasks.put(definitions, task);
            executor.execute(task);
        }
        if (inGroup != null) {
            synchronized (inGroup) {
                inGroup.getPending().put(definitions, task);
            }
        }
        return task;
    }

    public synchronized Task<Result, Definitions> executeNewTask(Definitions definitions) {
        Task<Result, Definitions> task = tasks.get(definitions);
        if (task == null) {
            task = createNewTask(definitions);
            tasks.put(definitions, task);
            task.run();
            return task;
        } else {
            throw new RuntimeException("Not implemented yet");
            // return task;
        }
    }

    public synchronized void cancel(Task<Result, Definitions> task) {
        task.cancel();
        finish(task);
    }

    protected synchronized void finish(Task<Result, Definitions> task) {
        Definitions definitions = task.getDefinitions();
        tasks.remove(task.getDefinitions());
        for (TaskGroupImpl<Result, Definitions> group : groups) {
            synchronized (group) {
                if (group.getPending().containsKey(definitions)) {
                    group.finish(task);
                }
            }
        }
    }
    
    public void markAsFinished(Task<Result, Definitions> task) {
        tasks.remove(task.getDefinitions());
    }
    
    protected abstract AbstractTask<Result, Definitions> createNewTask(Definitions definitions);
}
