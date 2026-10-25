package com.vyra.be_virtual_your_assets.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product metrics (PRD §13), aggregated only: counts and percentages, never the content of a member.
 * A rate is null when nobody is in its group yet.
 */
@Data
public class ProductMetricsResponse {
    private int periodDays;
    private LocalDateTime generatedAt;
    private Members members;
    private Activation activation;
    private List<Retention> retention;
    private Engagement engagement;
    private Reliability reliability;

    @Data
    public static class Members {
        private long active;
        private long newInPeriod;
    }

    /** New members of the period who did a first core action (transaction, mood, habit, goal, budget). */
    @Data
    public static class Activation {
        private long newMembers;
        private long activated;
        private BigDecimal rate;
        private long onboardingCompleted;
    }

    /** Members registered 90..N days ago who were active again N or more days after registering. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Retention {
        private int day;
        private long cohort;
        private long retained;
        private BigDecimal rate;
    }

    @Data
    public static class Engagement {
        /** Members with an active habit who completed one in the last 7 days. */
        private Rate habit;
        /** Members with an active goal who updated its progress in the period. */
        private Rate goal;
        /** Active members who recorded a transaction in the period. */
        private Rate finance;
        /** Active members who checked in their mood in the last 7 days. */
        private Rate mood;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rate {
        private long eligible;
        private long engaged;
        private BigDecimal rate;
    }

    /** Since this instance started (Micrometer); the dashboard of docs/monitoring.md covers longer periods. */
    @Data
    public static class Reliability {
        private long uptimeSeconds;
        private long requests;
        private long serverErrors;
        private BigDecimal errorRate;
        /** null until the histogram has data. */
        private Double p95Millis;
    }
}
