package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.service.AnnouncementService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sends published announcements as notifications once they start (every minute, one instance). */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnnouncementJob {

    private final AnnouncementService announcementService;
    private final TimeService timeService;

    @Scheduled(cron = "45 * * * * *")
    @SchedulerLock(name = "announcementJob", lockAtMostFor = "PT30M")
    public void scheduled() {
        int created = announcementService.deliverDue(timeService.nowUtc());
        if (created > 0) {
            log.info("Announcement notifications created: {}", created);
        }
    }
}
