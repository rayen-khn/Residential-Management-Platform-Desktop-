package com.syndicati.services.analytics;

import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.log.data.AppEventLogRepository;
import com.syndicati.models.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuspiciousActivityServiceTest {

    @Test
    void detectForUserFlagsRepeatedAuthFailures() {
        StubLogRepository repository = new StubLogRepository();
        User user = new User();
        user.setIdUser(42);

        repository.logs.add(logFor(user, "AUTH_FAILURE"));
        repository.logs.add(logFor(user, "AUTH_FAILURE"));
        repository.logs.add(logFor(user, "AUTH_FAILURE"));

        SuspiciousActivityService service = new SuspiciousActivityService(repository);
        SuspiciousActivityService.SuspiciousActivityResult result = service.detectForUser(42, null);

        assertTrue(result.isSuspicious());
        assertTrue(result.flags.stream().anyMatch(f -> "REPEATED_FAILED_AUTH".equals(f.ruleId)));
    }

    private static AppEventLog logFor(User user, String eventType) {
        AppEventLog log = new AppEventLog();
        log.setUser(user);
        log.setEventType(eventType);
        log.setEventTimestamp(LocalDateTime.now().minusMinutes(5));
        return log;
    }

    private static class StubLogRepository extends AppEventLogRepository {
        private final List<AppEventLog> logs = new ArrayList<>();

        @Override
        public List<AppEventLog> findLatest(int limit) {
            return logs;
        }
    }
}
