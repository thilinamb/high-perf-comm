package com.thilinamb.asyncserver.core;

import com.thilinamb.asyncserver.core.exception.ThreadPoolException;
import com.thilinamb.asyncserver.core.task.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Thilina Buddhika
 */
public class ThreadPool {
    private static ThreadPool instance;
    private static boolean initialized = false;

    private final ExecutorService threadPool;

    public ThreadPool(int threadPoolSize) {
        threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    static void initialize(int threadPoolSize){
        synchronized (ThreadPool.class) {
            instance = new ThreadPool(threadPoolSize);
            initialized = true;
        }
    }

    public static ThreadPool getInstance() throws ThreadPoolException {
        if(initialized){
            return instance;
        } else {
            throw new ThreadPoolException("Accessing an uninitialized thread pool.");
        }
    }

    public void submitTask(Task task){
        threadPool.submit(task);
    }

}
