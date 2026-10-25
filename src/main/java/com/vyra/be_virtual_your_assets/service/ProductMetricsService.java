package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.metrics.ProductMetricsResponse;
import com.vyra.be_virtual_your_assets.dto.metrics.SystemStatusResponse;
import com.vyra.be_virtual_your_assets.repository.ProductMetricsQueries;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Product metrics and service status for the portal (DASHBOARD_VIEW). Aggregates only: the portal never
 * sees what a member wrote, tracked or spent.
 */
@Service
@Slf4j
public class ProductMetricsService {

    static final int MIN_DAYS = 7;
    static final int MAX_DAYS = 90;
    static final int COHORT_DAYS = 90;
    static final int[] RETENTION_DAYS = {1, 7, 30};
    static final int OVERDUE_PUSH_MINUTES = 10;

    private final ProductMetricsQueries queries;
    private final MeterRegistry meterRegistry;
    private final SystemSettings systemSettings;
    private final PushClient pushClient;
    private final AdminAuthorizationService adminAuthorizationService;
    private final TimeService timeService;
    private final boolean schedulerEnabled;

    public ProductMetricsService(ProductMetricsQueries queries, MeterRegistry meterRegistry, SystemSettings systemSettings,
                                 PushClient pushClient, AdminAuthorizationService adminAuthorizationService,
                                 TimeService timeService,
                                 @Value("${vyra.scheduler.enabled:true}") boolean schedulerEnabled) {
        this.queries = queries;
        this.meterRegistry = meterRegistry;
        this.systemSettings = systemSettings;
        this.pushClient = pushClient;
        this.adminAuthorizationService = adminAuthorizationService;
        this.timeService = timeService;
        this.schedulerEnabled = schedulerEnabled;
    }

    /** Metrics of the last {@code days} days (7..90, default 30). */
    public BaseResponse<ProductMetricsResponse> getMetrics(Integer days) {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);
        int period = days == null ? 30 : Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        LocalDateTime now = timeService.nowUtc();
        LocalDateTime since = now.minusDays(period);

        ProductMetricsResponse response = new ProductMetricsResponse();
        response.setPeriodDays(period);
        response.setGeneratedAt(now);

        ProductMetricsResponse.Members members = new ProductMetricsResponse.Members();
        members.setActive(queries.activeMembers());
        members.setNewInPeriod(queries.newMembers(since));
        response.setMembers(members);

        ProductMetricsResponse.Activation activation = new ProductMetricsResponse.Activation();
        activation.setNewMembers(members.getNewInPeriod());
        activation.setActivated(queries.activatedMembers(since));
        activation.setRate(rate(activation.getActivated(), activation.getNewMembers()));
        activation.setOnboardingCompleted(queries.onboardedMembers(since));
        response.setActivation(activation);

        LocalDateTime cohortFrom = now.minusDays(COHORT_DAYS);
        response.setRetention(Arrays.stream(RETENTION_DAYS).mapToObj(day -> {
            long[] counts = queries.retention(cohortFrom, now.minusDays(day), day);
            return new ProductMetricsResponse.Retention(day, counts[0], counts[1], rate(counts[1], counts[0]));
        }).toList());

        ProductMetricsResponse.Engagement engagement = new ProductMetricsResponse.Engagement();
        engagement.setHabit(toRate(queries.habitEngagement(now.minusDays(7))));
        engagement.setGoal(toRate(queries.goalEngagement(since)));
        engagement.setFinance(toRate(queries.financeEngagement(since)));
        engagement.setMood(toRate(queries.moodEngagement(now.minusDays(7))));
        response.setEngagement(engagement);

        response.setReliability(reliability());
        return new BaseResponse<>(ErrorConstant.PRODUCT_METRICS_SUCCESS.getCode(),
                ErrorConstant.PRODUCT_METRICS_SUCCESS.getMessage(), response);
    }

    public BaseResponse<SystemStatusResponse> getStatus() {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);
        LocalDateTime now = timeService.nowUtc();
        SystemStatusResponse response = new SystemStatusResponse();
        response.setCheckedAt(now);
        response.setUptimeSeconds(uptimeSeconds());
        response.setMaintenance(systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED));

        long started = System.nanoTime();
        boolean databaseUp;
        try {
            queries.ping();
            databaseUp = true;
        } catch (RuntimeException e) {
            log.warn("Database check failed. reason: {}", e.getClass().getSimpleName());
            databaseUp = false;
        }
        response.setDatabase(new SystemStatusResponse.Database(databaseUp,
                databaseUp ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) : null));

        SystemStatusResponse.Scheduler scheduler = new SystemStatusResponse.Scheduler();
        scheduler.setEnabled(schedulerEnabled);
        SystemStatusResponse.Push push = new SystemStatusResponse.Push();
        push.setEnabled(pushClient.isEnabled());
        if (databaseUp) {
            scheduler.setJobs(queries.jobRuns().stream()
                    .map(row -> new SystemStatusResponse.Job((String) row[0], (LocalDateTime) row[1]))
                    .toList());
            push.setLastSentAt(queries.lastPushSentAt());
            push.setPending(queries.pendingPush());
            push.setOverdue(queries.overduePush(now.minusMinutes(OVERDUE_PUSH_MINUTES)));
            response.setPendingExports(queries.pendingExports());
        } else {
            scheduler.setJobs(List.of());
        }
        response.setScheduler(scheduler);
        response.setPush(push);
        response.setStatus(!databaseUp ? "DOWN"
                : response.isMaintenance() || push.getOverdue() > 0 ? "DEGRADED" : "UP");
        return new BaseResponse<>(ErrorConstant.SYSTEM_STATUS_SUCCESS.getCode(),
                ErrorConstant.SYSTEM_STATUS_SUCCESS.getMessage(), response);
    }

    private ProductMetricsResponse.Reliability reliability() {
        ProductMetricsResponse.Reliability reliability = new ProductMetricsResponse.Reliability();
        reliability.setUptimeSeconds(uptimeSeconds());
        long requests = 0;
        long errors = 0;
        double p95 = -1;
        for (Timer timer : meterRegistry.find("http.server.requests").timers()) {
            long count = timer.count();
            requests += count;
            String status = timer.getId().getTag("status");
            if (status != null && status.startsWith("5")) {
                errors += count;
            }
            for (ValueAtPercentile value : timer.takeSnapshot().percentileValues()) {
                if (value.percentile() == 0.95) {
                    p95 = Math.max(p95, value.value(TimeUnit.MILLISECONDS));
                }
            }
        }
        reliability.setRequests(requests);
        reliability.setServerErrors(errors);
        reliability.setErrorRate(rate(errors, requests));
        reliability.setP95Millis(p95 < 0 ? null : p95);
        return reliability;
    }

    private static long uptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }

    private static ProductMetricsResponse.Rate toRate(long[] counts) {
        return new ProductMetricsResponse.Rate(counts[0], counts[1], rate(counts[1], counts[0]));
    }

    /** Percent with 1 decimal; null when nobody is in the group. */
    static BigDecimal rate(long part, long whole) {
        return whole == 0 ? null
                : BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }
}
