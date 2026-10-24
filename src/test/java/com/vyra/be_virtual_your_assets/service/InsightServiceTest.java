package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.dto.insight.WeeklyInsightResponse;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-22 17:30 UTC = Friday 2026-10-23 00:30 in Jakarta. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 25);

    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private MoodEntryRepository moodEntryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ValidationService validationService;

    private InsightService insightService;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        insightService = new InsightService(habitRepository, completionRepository, moodEntryRepository,
                transactionRepository, validationService, timeService);
        Member member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
    }

    @Test
    void weekly_currentWeek_combinesHabitsMoodAndSpending() {
        // Daily habit, target 1: Mon-Thu are past (Mon, Tue, Thu done), today (Fri) not done yet -> 3 of 4.
        Habit daily = habit("h-1", Habit.EVERY_DAY);
        // Weekends only: nothing scheduled up to today.
        Habit weekend = habit("h-2", Habit.mask(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));
        Habit archived = habit("h-3", Habit.EVERY_DAY);
        archived.setArchivedAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        when(habitRepository.findAllOfMember(MEMBER)).thenReturn(List.of(daily, weekend, archived));
        when(completionRepository.findRange(MEMBER, MONDAY, SUNDAY)).thenReturn(List.of(
                completion("h-1", MONDAY), completion("h-1", MONDAY.plusDays(1)), completion("h-1", MONDAY.plusDays(3))));
        when(moodEntryRepository.findRange(MEMBER, MONDAY, SUNDAY)).thenReturn(List.of(mood(4), mood(3)));
        when(transactionRepository.sumByCategory(MEMBER, MONDAY, SUNDAY)).thenReturn(List.of(
                new Object[]{"c-1", "EXPENSE", new BigDecimal("-150000")},
                new Object[]{"c-2", "INCOME", new BigDecimal("900000")}));
        when(transactionRepository.sumByCategory(MEMBER, MONDAY.minusDays(7), SUNDAY.minusDays(7)))
                .thenReturn(List.<Object[]>of(new Object[]{"c-1", "EXPENSE", new BigDecimal("-100000")}));

        WeeklyInsightResponse result = insightService.getWeekly(MEMBER, null).getData();

        assertThat(result.getFrom()).isEqualTo(MONDAY);
        assertThat(result.getTo()).isEqualTo(SUNDAY);
        assertThat(result.getHabits().getScheduled()).isEqualTo(4);
        assertThat(result.getHabits().getDone()).isEqualTo(3);
        assertThat(result.getHabits().getPercent()).isEqualTo(75);
        assertThat(result.getMood().getAverage()).isEqualByComparingTo("3.50");
        assertThat(result.getMood().getEntries()).isEqualTo(2);
        assertThat(result.getExpense().getThisWeek()).isEqualByComparingTo("150000");
        assertThat(result.getExpense().getLastWeek()).isEqualByComparingTo("100000");
        assertThat(result.getExpense().getChangePercent()).isEqualByComparingTo("50.0");
    }

    @Test
    void weekly_withoutData_isEmptyNotZero() {
        WeeklyInsightResponse result = insightService.getWeekly(MEMBER, TODAY.minusDays(7)).getData();

        assertThat(result.getFrom()).isEqualTo(MONDAY.minusDays(7));
        assertThat(result.getHabits().getPercent()).isNull();
        assertThat(result.getMood().getAverage()).isNull();
        assertThat(result.getExpense().getThisWeek()).isEqualByComparingTo("0");
        assertThat(result.getExpense().getChangePercent()).isNull();
    }

    private static Habit habit(String id, short mask) {
        Habit habit = new Habit();
        habit.setId(id);
        habit.setMemberId(MEMBER);
        habit.setTargetCount(1);
        habit.setScheduleDays(mask);
        habit.setStartDate(LocalDate.of(2026, 9, 1));
        return habit;
    }

    private static HabitCompletion completion(String habitId, LocalDate day) {
        HabitCompletion completion = new HabitCompletion();
        completion.setHabitId(habitId);
        completion.setMemberId(MEMBER);
        completion.setLocalDate(day);
        completion.setCount(1);
        return completion;
    }

    private static MoodEntry mood(int score) {
        MoodEntry entry = new MoodEntry();
        entry.setMoodScore((short) score);
        return entry;
    }
}
