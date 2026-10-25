package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.dto.metrics.ProductMetricsResponse;
import com.vyra.be_virtual_your_assets.dto.metrics.SystemStatusResponse;
import com.vyra.be_virtual_your_assets.repository.ProductMetricsQueries;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductMetricsServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 3, 0);

    @Mock private ProductMetricsQueries queries;
    @Mock private SystemSettings systemSettings;
    @Mock private PushClient pushClient;
    @Mock private AdminAuthorizationService adminAuthorizationService;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ProductMetricsService service;

    @BeforeEach
    void setUp() {
        service = new ProductMetricsService(queries, meterRegistry, systemSettings, pushClient, adminAuthorizationService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"), true);
    }

    @Test
    void getMetrics_aggregatesRatesAndClampsThePeriod() {
        when(queries.activeMembers()).thenReturn(120L);
        when(queries.newMembers(NOW.minusDays(90))).thenReturn(40L);
        when(queries.activatedMembers(NOW.minusDays(90))).thenReturn(10L);
        when(queries.onboardedMembers(NOW.minusDays(90))).thenReturn(30L);
        when(queries.retention(eq(NOW.minusDays(90)), any(), anyInt())).thenReturn(new long[]{20, 5});
        when(queries.habitEngagement(NOW.minusDays(7))).thenReturn(new long[]{8, 6});
        when(queries.goalEngagement(NOW.minusDays(90))).thenReturn(new long[]{0, 0});
        when(queries.financeEngagement(NOW.minusDays(90))).thenReturn(new long[]{120, 30});
        when(queries.moodEngagement(NOW.minusDays(7))).thenReturn(new long[]{120, 3});

        ProductMetricsResponse result = service.getMetrics(365).getData();

        verify(adminAuthorizationService).requirePermission(AdminPermission.DASHBOARD_VIEW);
        assertThat(result.getPeriodDays()).isEqualTo(90);
        assertThat(result.getMembers().getActive()).isEqualTo(120);
        assertThat(result.getActivation().getRate()).isEqualByComparingTo("25.0");
        assertThat(result.getRetention()).extracting(ProductMetricsResponse.Retention::getDay).containsExactly(1, 7, 30);
        assertThat(result.getRetention().get(0).getRate()).isEqualByComparingTo("25.0");
        assertThat(result.getEngagement().getHabit().getRate()).isEqualByComparingTo("75.0");
        assertThat(result.getEngagement().getGoal().getRate()).isNull();
        assertThat(result.getEngagement().getMood().getRate()).isEqualByComparingTo("2.5");
        assertThat(result.getReliability().getRequests()).isZero();
        assertThat(result.getReliability().getErrorRate()).isNull();
    }

    @Test
    void rate_isOneDecimalAndNullForAnEmptyGroup() {
        assertThat(ProductMetricsService.rate(1, 3)).isEqualByComparingTo(new BigDecimal("33.3"));
        assertThat(ProductMetricsService.rate(0, 0)).isNull();
    }

    @Test
    void getStatus_databaseDown_isDownWithoutFurtherQueries() {
        doThrow(new DataAccessResourceFailureException("down")).when(queries).ping();

        SystemStatusResponse result = service.getStatus().getData();

        assertThat(result.getStatus()).isEqualTo("DOWN");
        assertThat(result.getDatabase().isUp()).isFalse();
        assertThat(result.getScheduler().getJobs()).isEmpty();
        verify(queries, never()).pendingPush();
    }

    @Test
    void getStatus_overduePush_isDegraded() {
        when(pushClient.isEnabled()).thenReturn(true);
        when(queries.jobRuns()).thenReturn(List.<Object[]>of(new Object[]{"pushDispatchJob", NOW.minusMinutes(1)}));
        when(queries.lastPushSentAt()).thenReturn(NOW.minusMinutes(2));
        when(queries.pendingPush()).thenReturn(4L);
        when(queries.overduePush(NOW.minusMinutes(ProductMetricsService.OVERDUE_PUSH_MINUTES))).thenReturn(2L);
        when(queries.pendingExports()).thenReturn(1L);
        when(systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED)).thenReturn(false);

        SystemStatusResponse result = service.getStatus().getData();

        assertThat(result.getStatus()).isEqualTo("DEGRADED");
        assertThat(result.getScheduler().getJobs()).hasSize(1);
        assertThat(result.getPush().getPending()).isEqualTo(4);
        assertThat(result.getPendingExports()).isEqualTo(1);
    }
}
