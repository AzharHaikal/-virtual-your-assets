package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.service.NotificationMessages;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "Due in 3 days" for active goals, from 08:00 in the member's own zone. Runs every hour; the dedup key
 * GOAL_DUE:{goalId}:{dueDate} keeps it to one notification per goal and due date.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoalDueJob {

    static final int DAYS_BEFORE = 3;
    static final int SEND_FROM_HOUR = 8;

    private final GoalRepository goalRepository;
    private final NotificationService notificationService;
    private final TimeService timeService;

    @Scheduled(cron = "0 5 * * * *")
    @SchedulerLock(name = "goalDueJob", lockAtMostFor = "PT30M")
    public void scheduled() {
        run(timeService.nowUtc());
    }

    /** @return notifications created */
    public int run(LocalDateTime nowUtc) {
        // Every zone's "today" is within a day of the UTC date.
        LocalDate utcDay = nowUtc.toLocalDate();
        int created = 0;
        for (Object[] row : goalRepository.findActiveDueBetween(
                utcDay.plusDays(DAYS_BEFORE - 1L), utcDay.plusDays(DAYS_BEFORE + 1L))) {
            Goal goal = (Goal) row[0];
            Member member = (Member) row[1];
            ZonedDateTime local = nowUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(timeService.zoneOf(member));
            if (local.getHour() < SEND_FROM_HOUR || !goal.getDueDate().equals(local.toLocalDate().plusDays(DAYS_BEFORE))) {
                continue;
            }
            try {
                if (notificationService.notify(member, NotificationType.GOAL_DUE,
                        "GOAL_DUE:" + goal.getId() + ":" + goal.getDueDate(),
                        NotificationMessages.goalDue(member.getLanguage(), goal.getTitle(), goal.getDueDate()),
                        payload(goal))) {
                    created++;
                }
            } catch (RuntimeException e) {
                log.warn("Goal due notification failed. goalId: {} reason: {}", goal.getId(), e.getClass().getSimpleName());
            }
        }
        if (created > 0) {
            log.info("Goal due notifications created: {}", created);
        }
        return created;
    }

    private static Map<String, String> payload(Goal goal) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("route", "goal");
        payload.put("goalId", goal.getId());
        return payload;
    }
}
