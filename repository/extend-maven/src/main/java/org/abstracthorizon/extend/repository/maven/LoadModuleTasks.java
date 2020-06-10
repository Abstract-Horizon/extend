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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.abstracthorizon.extend.Extend;
import org.abstracthorizon.extend.repository.concurent.AbstractConcurrentTasks;
import org.abstracthorizon.extend.repository.concurent.Task;
import org.abstracthorizon.extend.repository.concurent.TaskGroup;
import org.abstracthorizon.extend.server.deployment.Module;

/**
 * Manager that handles loading modules.
 * 
 * @author Daniel Sendula
 */
public class LoadModuleTasks extends AbstractConcurrentTasks<Module, LoadModuleTaskDefinitions> {

    protected ParallelRepositoryModuleLoader loader;
    
    protected HashMap<LoadModuleTaskDefinitions, Task<Module, LoadModuleTaskDefinitions>> notCompleted 
                        = new HashMap<LoadModuleTaskDefinitions, Task<Module, LoadModuleTaskDefinitions>>();
    
    public LoadModuleTasks(ParallelRepositoryModuleLoader loader, Executor executor) {
        super(executor);
        this.loader = loader;
    }

    public synchronized Task<Module, LoadModuleTaskDefinitions> submitNewTask(TaskGroup<Module, LoadModuleTaskDefinitions> inGroup, LoadModuleTaskDefinitions definitions) {
        Task<Module, LoadModuleTaskDefinitions> task = notCompleted.get(definitions);
        if (task != null) {
            if (Extend.debug.isInfoEnabled()) {
                Extend.debug.info(definitions.getArtifact() + ": found finished task. Will wait on it.");
            }
            return task;
        } else {
            return super.submitNewTask(inGroup, definitions);
        }
    }
    
    public synchronized Task<Module, LoadModuleTaskDefinitions> executeNewTask(LoadModuleTaskDefinitions definitions) {
        Task<Module, LoadModuleTaskDefinitions> task = notCompleted.get(definitions);
        if (task != null) {
            throw new RuntimeException("Not yet implemented");
        } else {
            return super.executeNewTask(definitions);
        }
    }    
    
    protected LoadModuleTask createNewTask(LoadModuleTaskDefinitions definitions) {
        LoadModuleTask task = new LoadModuleTask(this, definitions, loader);
        return task;
    }

    protected synchronized void finish(Task<Module, LoadModuleTaskDefinitions> task) {
        super.finish(task);
        notCompleted.put(task.getDefinitions(), task);
    }
    
    public void complete(Task<Module, LoadModuleTaskDefinitions> task) {
        notCompleted.remove(task.getDefinitions());
    }

    public Map<LoadModuleTaskDefinitions, Task<Module, LoadModuleTaskDefinitions>> getNotCompleted() {
        return notCompleted;
    }
}
