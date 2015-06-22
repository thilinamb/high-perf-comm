package com.thilinamb.asyncserver.core.task;

import java.nio.channels.SelectionKey;

/**
 * @author Thilina Buddhika
 */
public abstract class Task implements Runnable {

    protected SelectionKey selectionKey;

    public Task(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }
}
