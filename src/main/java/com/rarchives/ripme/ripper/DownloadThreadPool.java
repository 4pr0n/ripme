package com.rarchives.ripme.ripper;

import com.rarchives.ripme.utils.Utils;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple wrapper around a FixedThreadPool.
 */
public class DownloadThreadPool {

    private static final Logger LOGGER = Logger.getLogger(DownloadThreadPool.class);
    private ThreadPoolExecutor threadPool = null;

    public DownloadThreadPool() {
        initialize("Main");
    }

    public DownloadThreadPool(String threadPoolName) {
        initialize(threadPoolName);
    }

    private void initialize(String threadPoolName) {
        int threads = Utils.getConfigInteger("threads.size", 10);
        LOGGER.debug("Initializing " + threadPoolName + " thread pool with " + threads + " threads");
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
    }

    public void addThread(Thread t) {
        threadPool.execute(t);
    }

    public void waitForThreads() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(3600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("[!] Interrupted while waiting for threads to finish: ", e);
        }
    }
}
