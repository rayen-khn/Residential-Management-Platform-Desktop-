package com.syndicati.services.observability;

/**
 * Application-level lifecycle manager for Langfuse tracing.
 *
 * Ensures Langfuse configuration/tracer are initialized once and started
 * alongside the desktop application lifecycle.
 */
public class LangfuseRuntimeService {

    private static final String LOG_TAG = "[LangfuseRuntimeService]";
    private static final LangfuseRuntimeService INSTANCE = new LangfuseRuntimeService();

    private volatile boolean started;
    private LangfuseConfig config;
    private LangfuseTracer tracer;

    private LangfuseRuntimeService() {
    }

    public static LangfuseRuntimeService getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        config = new LangfuseConfig();
        tracer = new LangfuseTracer(config);
        started = true;

        if (config.isEnabled()) {
            boolean hasPublicKey = config.getPublicKey() != null && !config.getPublicKey().isBlank();
            boolean hasSecretKey = config.getSecretKey() != null && !config.getSecretKey().isBlank();
            System.out.println(LOG_TAG + " Started. baseUrl=" + config.getBaseUrl()
                    + ", publicKey=" + (hasPublicKey ? "present" : "missing")
                    + ", secretKey=" + (hasSecretKey ? "present" : "missing"));
        } else {
            System.out.println(LOG_TAG + " Started in disabled mode (langfuse.enabled=false).");
        }
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }

        if (tracer != null) {
            tracer.clearContext();
        }

        started = false;
        System.out.println(LOG_TAG + " Stopped.");
    }

    public boolean isStarted() {
        return started;
    }

    public synchronized boolean isEnabled() {
        if (!started) {
            start();
        }
        return config != null && config.isEnabled();
    }

    public synchronized LangfuseTracer tracer() {
        if (!started) {
            start();
        }
        return tracer;
    }

    public synchronized String diagnosticSummary() {
        if (!started) {
            start();
        }

        boolean hasPublicKey = config.getPublicKey() != null && !config.getPublicKey().isBlank();
        boolean hasSecretKey = config.getSecretKey() != null && !config.getSecretKey().isBlank();
        return "started=" + started
                + ", enabled=" + config.isEnabled()
                + ", baseUrl=" + config.getBaseUrl()
                + ", publicKey=" + (hasPublicKey ? "present" : "missing")
                + ", secretKey=" + (hasSecretKey ? "present" : "missing");
    }
}
