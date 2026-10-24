package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.service.NotificationMessages;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Habit reminders at the member's local time. Every minute, per time zone, the habits whose reminder time
 * fell in the last few minutes (so a missed run is caught up) are reminded when scheduled today and not
 * done yet. Archived / deleted habits and inactive members are never reminded. The dedup key
 * HABIT:{habitId}:{localDate} keeps it to one reminder per habit and day, also with several instances.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HabitReminderJob {

    /** Reminder times in (now - WINDOW, now] are due. */
    static final int WINDOW_MINUTES = 5;

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final NotificationService notificationService;
    private final TimeService timeService;

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "habitReminderJob", lockAtMostFor = "PT5M")
    public void scheduled() {
        run(timeService.nowUtc());
    }

    /** @return reminders created */
    public int run(LocalDateTime nowUtc) {
        int created = 0;
        for (String zoneId : habitRepository.findReminderZones()) {
            ZonedDateTime local = nowUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone(zoneId));
            LocalTime to = local.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime from = to.minusMinutes(WINDOW_MINUTES);
            List<Object[]> rows = from.isBefore(to)
                    ? habitRepository.findReminderCandidates(zoneId, from, to)
                    // Just after midnight only today's part of the window: yesterday's late reminders are dropped.
                    : habitRepository.findReminderCandidatesUpTo(zoneId, to);
            created += remind(rows, local.toLocalDate());
        }
        if (created > 0) {
            log.info("Habit reminders created: {}", created);
        }
        return created;
    }

    private int remind(List<Object[]> rows, LocalDate today) {
        int created = 0;
        for (Object[] row : rows) {
            Habit habit = (Habit) row[0];
            Member member = (Member) row[1];
            if (!habit.isScheduledOn(today) || isDone(habit, today)) {
                continue;
            }
            try {
                if (notificationService.notify(member, NotificationType.HABIT_REMINDER,
                        "HABIT:" + habit.getId() + ":" + today,
                        NotificationMessages.habitReminder(member.getLanguage(), habit.getName()),
                        payload(habit, today))) {
                    created++;
                }
            } catch (RuntimeException e) {
                log.warn("Habit reminder failed. habitId: {} reason: {}", habit.getId(), e.getClass().getSimpleName());
            }
        }
        return created;
    }

    private boolean isDone(Habit habit, LocalDate day) {
        return completionRepository.findByHabitIdAndLocalDate(habit.getId(), day)
                .map(HabitCompletion::getCount)
                .filter(count -> count >= habit.getTargetCount())
                .isPresent();
    }

    private ZoneId zone(String zoneId) {
        try {
            return ZoneId.of(zoneId);
        } catch (DateTimeException | NullPointerException e) {
            return timeService.defaultZone();
        }
    }

    static Map<String, String> payload(Habit habit, LocalDate day) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("route", "habit");
        payload.put("habitId", habit.getId());
        payload.put("localDate", day.toString());
        return payload;
    }
}
