package com.syndicati.services.observability;

import com.syndicati.models.log.AppEventLog;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenObserveExporterTest {

    @Test
    void exportDoesNothingWhenDisabled() {
        OpenObserveConfig config = new OpenObserveConfig();
        config.setEnabled(false);

        OpenObserveExporter exporter = new OpenObserveExporter(config);
        try {
            exporter.export(new AppEventLog());
            assertEquals(0, exporter.getQueueSize());
        } finally {
            exporter.shutdown();
        }
    }

    @Test
    void circuitBreakerOpensAfterConsecutiveErrors() throws Exception {
        OpenObserveConfig config = new OpenObserveConfig();
        config.setEnabled(true);

        OpenObserveExporter exporter = new OpenObserveExporter(config);
        try {
            Method handleError = OpenObserveExporter.class.getDeclaredMethod(
                    "handleError", List.class, int.class, String.class, String.class
            );
            handleError.setAccessible(true);

            for (int i = 0; i < 5; i++) {
                handleError.invoke(exporter, List.of(), 3, "forced-error", "");
            }

            assertTrue(exporter.isCircuitBreakerOpen());
        } finally {
            exporter.shutdown();
        }
    }
}
