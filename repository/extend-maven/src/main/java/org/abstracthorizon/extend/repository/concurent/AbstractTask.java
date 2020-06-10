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

import org.abstracthorizon.extend.Extend;

/**
 *
 *
 * @author Daniel Sendula
 */
public abstract class AbstractTask<Result, Definitions> implements Task<Result, Definitions>, Runnable {

    private AbstractConcurrentTasks<Result, Definitions> owner;
    private Definitions definitions;
    private Result result;
    private Throwable exception;
    private Thread thread;
    
    public AbstractTask(AbstractConcurrentTasks<Result, Definitions> owner, Definitions definitions) {
        this.owner = owner;
        this.definitions = definitions;
    }
    
    public Definitions getDefinitions() {
        return definitions;
    }

    public Throwable getException() {
        return exception;
    }
    
    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
    
    protected AbstractConcurrentTasks<Result, Definitions> getOwner() {
        return owner;
    }

    public void cancel() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    protected void done() {
        owner.finish(this);
    }
    
    public void run() {
        thread = Thread.currentThread();
        Thread.interrupted(); // Clear interrupted flag
        try {
            execute();
            done();
            if (Thread.interrupted()) {
                Extend.debug.warn("Task continued to work after being interrupted!" + getDefinitions());
            }
        } catch (Throwable t) {
            if (t instanceof InterruptedException) {
                // What now?
                exception = t;
            } else {
                exception = t;
            }
            owner.finish(this);
        }
    }
    
    public int hashCode() {
        return getDefinitions().hashCode();
    }
    
    public boolean equals(Object o) {
        if (o instanceof AbstractTask) {
            AbstractTask<?, ?> otherTask =(AbstractTask<?, ?>)o;
            return getDefinitions().equals(otherTask.getDefinitions());
        }
        return super.equals(o);
    }    

    public String toString() {
        String s = getClass().getName();
        int i = s.lastIndexOf('.');
        if (i >= 0) {
            s = s.substring(i + 1);
        }
        return s + "[" + getDefinitions().toString() + "]";
    }
    
    protected abstract void execute();
}
