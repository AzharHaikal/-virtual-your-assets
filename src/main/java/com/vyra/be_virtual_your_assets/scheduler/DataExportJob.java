package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import com.vyra.be_virtual_your_assets.repository.DataExportRepository;
import com.vyra.be_virtual_your_assets.service.DataExportService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data exports in the background: builds the queued ones (a few per minute) and empties the expired
 * ones every hour. An export left PROCESSING by an instance that stopped is queued again.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataExportJob {

    static final int BATCH_SIZE = 3;
    static final int STUCK_MINUTES = 30;

    private final DataExportRepository exportRepository;
    private final DataExportService dataExportService;
    private final TimeService timeService;

    @Scheduled(cron = "15 * * * * *")
    @SchedulerLock(name = "dataExportJob", lockAtMostFor = "PT30M")
    public void scheduled() {
        run(timeService.nowUtc());
    }

    @Scheduled(cron = "0 20 * * * *")
    @SchedulerLock(name = "dataExportCleanupJob", lockAtMostFor = "PT10M")
    public void cleanup() {
        int expired = exportRepository.expire(timeService.nowUtc());
        if (expired > 0) {
            log.info("Data exports expired: {}", expired);
        }
    }

    /** @return exports processed (each in its own transactions, so one failure does not undo the others) */
    public int run(LocalDateTime nowUtc) {
        exportRepository.requeueStuck(nowUtc.minusMinutes(STUCK_MINUTES));
        List<String> queued = exportRepository.findIdsByStatus(DataExportStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        queued.forEach(dataExportService::process);
        return queued.size();
    }
}
