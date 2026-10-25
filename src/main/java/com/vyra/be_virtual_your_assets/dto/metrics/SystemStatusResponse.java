package com.vyra.be_virtual_your_assets.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Service status for the portal: database, scheduled jobs, push, exports and maintenance. */
@Data
public class SystemStatusResponse {
    /** UP, DEGRADED (maintenance on, push backlog) or DOWN (database not answering). */
    private String status;
    private LocalDateTime checkedAt;
    private long uptimeSeconds;
    private boolean maintenance;
    private Database database;
    private Scheduler scheduler;
    private Push push;
    private long pendingExports;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Database {
        private boolean up;
        private Long latencyMillis;
    }

    @Data
    public static class Scheduler {
        private boolean enabled;
        private List<Job> jobs;
    }

    /** Last run of a job (ShedLock), on any instance. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job {
        private String name;
        private LocalDateTime lastRunAt;
    }

    @Data
    public static class Push {
        /** Firebase credentials configured. */
        private boolean enabled;
        private LocalDateTime lastSentAt;
        private long pending;
        /** Pending deliveries whose time came more than 10 minutes ago. */
        private long overdue;
    }
}
