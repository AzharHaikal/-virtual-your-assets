package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitReminderJobTest {

    /** Thursday 2026-10-22, 00:30 UTC = 07:30 in Jakarta (WIB) and 08:30 in Makassar (WITA). */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 0, 30);
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 22);

    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private NotificationService notificationService;

    private HabitReminderJob job;

    @BeforeEach
    void setUp() {
        job = new HabitReminderJob(habitRepository, completionRepository, notificationService,
                new TimeService(Clock.systemUTC(), "Asia/Jakarta"));
    }

    @Test
    void remindsAtTheMembersLocalTime_perTimeZone() {
        Member wib = member("m-wib", "Asia/Jakarta");
        Member wita = member("m-wita", "Asia/Makassar");
        Habit morning = habit("h-1", Habit.EVERY_DAY, LocalTime.of(7, 30));
        Habit witaMorning = habit("h-2", Habit.EVERY_DAY, LocalTime.of(8, 30));
        when(habitRepository.findReminderZones()).thenReturn(List.of("Asia/Jakarta", "Asia/Makassar"));
        when(habitRepository.findReminderCandidates("Asia/Jakarta", LocalTime.of(7, 25), LocalTime.of(7, 30)))
                .thenReturn(List.<Object[]>of(new Object[]{morning, wib}));
        when(habitRepository.findReminderCandidates("Asia/Makassar", LocalTime.of(8, 25), LocalTime.of(8, 30)))
                .thenReturn(List.<Object[]>of(new Object[]{witaMorning, wita}));
        when(notificationService.notify(any(), any(), any(), any(), any())).thenReturn(true);

        assertThat(job.run(NOW)).isEqualTo(2);

        verify(notificationService).notify(eq(wib), eq(NotificationType.HABIT_REMINDER), eq("HABIT:h-1:2026-10-22"), any(),
                eq(HabitReminderJob.payload(morning, THURSDAY)));
        verify(notificationService).notify(eq(wita), eq(NotificationType.HABIT_REMINDER), eq("HABIT:h-2:2026-10-22"), any(),
                anyMap());
    }

    @Test
    void notScheduledToday_orAlreadyDone_isNotReminded() {
        Member wib = member("m-wib", "Asia/Jakarta");
        Habit weekends = habit("h-1", Habit.mask(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)), LocalTime.of(7, 30));
        Habit done = habit("h-2", Habit.EVERY_DAY, LocalTime.of(7, 28));
        when(habitRepository.findReminderZones()).thenReturn(List.of("Asia/Jakarta"));
        when(habitRepository.findReminderCandidates("Asia/Jakarta", LocalTime.of(7, 25), LocalTime.of(7, 30)))
                .thenReturn(List.of(new Object[]{weekends, wib}, new Object[]{done, wib}));
        HabitCompletion completion = new HabitCompletion();
        completion.setCount(1);
        when(completionRepository.findByHabitIdAndLocalDate("h-2", THURSDAY)).thenReturn(Optional.of(completion));

        assertThat(job.run(NOW)).isZero();
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    void justAfterMidnight_onlyTodaysReminders_areLookedUp() {
        when(habitRepository.findReminderZones()).thenReturn(List.of("Asia/Jakarta"));

        // 17:02 UTC = 00:02 WIB.
        job.run(LocalDateTime.of(2026, 10, 22, 17, 2));

        verify(habitRepository).findReminderCandidatesUpTo("Asia/Jakarta", LocalTime.of(0, 2));
        verify(habitRepository, never()).findReminderCandidates(any(), any(), any());
    }

    @Test
    void aSecondRun_isHarmless_becauseOfTheDedupKey() {
        Member wib = member("m-wib", "Asia/Jakarta");
        Habit morning = habit("h-1", Habit.EVERY_DAY, LocalTime.of(7, 30));
        when(habitRepository.findReminderZones()).thenReturn(List.of("Asia/Jakarta"));
        when(habitRepository.findReminderCandidates(eq("Asia/Jakarta"), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{morning, wib}));
        when(notificationService.notify(any(), any(), eq("HABIT:h-1:2026-10-22"), any(), any()))
                .thenReturn(true).thenReturn(false);

        assertThat(job.run(NOW)).isEqualTo(1);
        assertThat(job.run(NOW.plusMinutes(1))).isZero();
    }

    private static Member member(String id, String zone) {
        Member member = new Member();
        member.setId(id);
        member.setTimezone(zone);
        member.setLanguage(AppLanguage.ID);
        return member;
    }

    private static Habit habit(String id, short mask, LocalTime reminder) {
        Habit habit = new Habit();
        habit.setId(id);
        habit.setName("Minum air");
        habit.setTargetCount(1);
        habit.setScheduleDays(mask);
        habit.setStartDate(LocalDate.of(2026, 10, 1));
        habit.setReminderEnabled(true);
        habit.setReminderTime(reminder);
        return habit;
    }
}
