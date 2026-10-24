package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import com.vyra.be_virtual_your_assets.repository.DataExportRepository;
import com.vyra.be_virtual_your_assets.service.DataExportService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportJobTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 5, 0);

    @Mock private DataExportRepository exportRepository;
    @Mock private DataExportService dataExportService;

    @Test
    void run_requeuesStuckExports_thenBuildsTheQueuedOnes() {
        DataExportJob job = new DataExportJob(exportRepository, dataExportService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
        when(exportRepository.findIdsByStatus(eq(DataExportStatus.PENDING), any())).thenReturn(List.of("e-1", "e-2"));

        assertThat(job.run(NOW)).isEqualTo(2);

        verify(exportRepository).requeueStuck(NOW.minusMinutes(DataExportJob.STUCK_MINUTES));
        verify(dataExportService).process("e-1");
        verify(dataExportService).process("e-2");

        job.cleanup();
        verify(exportRepository).expire(NOW);
    }
}
