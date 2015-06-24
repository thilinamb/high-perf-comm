package com.thilinamb.asyncserver.core.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Thilina Buddhika
 */
public class StatCollector {

    private final Logger logger = LogManager.getLogger(StatCollector.class);

    private static StatCollector instance = new StatCollector();

    private AtomicLong sentCounter = new AtomicLong(0);
    private AtomicLong sentBytes = new AtomicLong(0);
    public static final int limit = 10000000;

    private long previousTimeStamp = -1;

    private StatCollector(){

    }

    public static StatCollector getInstance(){
        return instance;
    }

    private void incrementCounter(){
        synchronized (this) {
            if(sentCounter.getAndIncrement() % limit == 0){
                if(previousTimeStamp == -1){
                    previousTimeStamp = System.currentTimeMillis();
                } else {
                    long currentTimeStamp = System.currentTimeMillis();
                    long timeElapsed = currentTimeStamp - previousTimeStamp;
                    logger.info("Sent " + limit + " messages in " + timeElapsed + "ms. " +
                            "Throughput: " + ((double)limit*1000)/timeElapsed +
                            ", Data Rate(MB/s):" + ((sentBytes.get()*1000)/((1024*1024)*timeElapsed)));
                    sentBytes.set(0);
                    previousTimeStamp = currentTimeStamp;
                }
            }
        }
    }

    public void updateStatistics(long bytes){
        sentBytes.addAndGet(bytes);
        incrementCounter();
    }

}
