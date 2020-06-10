package org.abstracthorizon.extend.repo.actors;

import java.util.LinkedList;
import java.util.Queue;

public class Channel<T> {
    
    private Queue<T> inputQueue = new LinkedList<T>();
    
    public T receive() {
        synchronized (inputQueue) {
            while (inputQueue.isEmpty()) {
                try {
                    inputQueue.wait();
                } catch (InterruptedException ignore) { }
            }
            return inputQueue.poll();
        }
    }

    public boolean hasResults() {
        synchronized (inputQueue) {
            return !inputQueue.isEmpty();
        }
    }
    
    public void send(T t) {
        synchronized (inputQueue) {
            inputQueue.add(t);
            inputQueue.notify();
        }
    }

}
