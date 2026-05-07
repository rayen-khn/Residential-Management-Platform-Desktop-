package com.syndicati.services.analytics;

import com.syndicati.services.observability.LogAIConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic scheduler for anomaly scoring jobs.
 */
public class AnomalyScoringScheduler {

    private static final String LOG_TAG = "[AnomalyScoringScheduler]";

    private final LogAIConfig config;
    private final AnomalyScoreService anomalyScoreService;
    private ScheduledExecutorService scheduler;

    public AnomalyScoringScheduler() {
        this(new LogAIConfig(), new AnomalyScoreService());
    }

    public AnomalyScoringScheduler(LogAIConfig config, AnomalyScoreService anomalyScoreService) {
        this.config = config;
        this.anomalyScoreService = anomalyScoreService;
    }

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        if (!config.isEnabled()) {
            System.out.println(LOG_TAG + " Disabled by configuration.");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AnomalyScoringScheduler-Thread");
            t.setDaemon(true);
            return t;
        });

        int period = Math.max(30, config.getScheduleIntervalSeconds());
        scheduler.scheduleAtFixedRate(this::runCycle, 10, period, TimeUnit.SECONDS);
        System.out.println(LOG_TAG + " Started with interval=" + period + "s, batchSize=" + config.getBatchSize());
    }

    private void runCycle() {
        try {
            AnomalyScoreService.ScoreSummary summary = anomalyScoreService.scoreRecentEvents();
            if (summary.processed > 0) {
                System.out.println(LOG_TAG + " processed=" + summary.processed
                        + ", updated=" + summary.updated
                        + ", anomalies=" + summary.anomaliesDetected
                        + ", fallback=" + summary.usedFallback);
            }
        } catch (Exception e) {
            System.out.println(LOG_TAG + " Cycle failed: " + e.getMessage());
        }
    }

    public synchronized void stop() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
