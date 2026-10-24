package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.entity.Habit;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HabitStreakCalculatorTest {

    /** Friday. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final LocalDate FROM = TODAY.minusDays(400);

    @Test
    void dailyHabit_countsTheRunUpToToday_andTodayNotDoneDoesNotBreakIt() {
        Habit habit = habit(Habit.EVERY_DAY, LocalDate.of(2026, 10, 1));
        Set<LocalDate> done = Set.of(TODAY.minusDays(3), TODAY.minusDays(2), TODAY.minusDays(1));

        HabitStreakCalculator.Streak streak = HabitStreakCalculator.compute(habit, done, TODAY, FROM);

        assertThat(streak.current()).isEqualTo(3);
        assertThat(streak.best()).isEqualTo(3);

        Set<LocalDate> withToday = Set.of(TODAY.minusDays(1), TODAY);
        assertThat(HabitStreakCalculator.compute(habit, withToday, TODAY, FROM).current()).isEqualTo(2);
    }

    @Test
    void aMissedScheduledDay_endsTheStreak_butTheBestIsKept() {
        Habit habit = habit(Habit.EVERY_DAY, LocalDate.of(2026, 10, 1));
        Set<LocalDate> done = Set.of(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 13), // 4 in a row, then the 14th is missed
                TODAY.minusDays(1));

        HabitStreakCalculator.Streak streak = HabitStreakCalculator.compute(habit, done, TODAY, FROM);

        assertThat(streak.current()).isEqualTo(1);
        assertThat(streak.best()).isEqualTo(4);
    }

    @Test
    void specificDays_unscheduledDaysAreSkipped_andNeverBreakTheStreak() {
        // Monday, Wednesday, Friday.
        Habit habit = habit(Habit.mask(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                LocalDate.of(2026, 10, 1));
        Set<LocalDate> done = Set.of(
                LocalDate.of(2026, 10, 16), // Fri
                LocalDate.of(2026, 10, 19), // Mon
                LocalDate.of(2026, 10, 21)); // Wed; today (Fri 23) not done yet

        HabitStreakCalculator.Streak streak = HabitStreakCalculator.compute(habit, done, TODAY, FROM);

        assertThat(streak.current()).isEqualTo(3);
    }

    @Test
    void daysBeforeTheStartOrTheLookBack_areIgnored() {
        Habit habit = habit(Habit.EVERY_DAY, TODAY.minusDays(1));
        // A done day before the habit started does not count; yesterday (the first day) was missed.
        HabitStreakCalculator.Streak streak = HabitStreakCalculator.compute(habit, Set.of(TODAY.minusDays(5)), TODAY, FROM);
        assertThat(streak.current()).isZero();
        assertThat(streak.best()).isZero();

        Habit old = habit(Habit.EVERY_DAY, LocalDate.of(2020, 1, 1));
        LocalDate shortFrom = TODAY.minusDays(2);
        Set<LocalDate> everyDay = Set.of(TODAY.minusDays(3), TODAY.minusDays(2), TODAY.minusDays(1), TODAY);
        assertThat(HabitStreakCalculator.compute(old, everyDay, TODAY, shortFrom).current()).isEqualTo(3);
    }

    private static Habit habit(short mask, LocalDate start) {
        Habit habit = new Habit();
        habit.setId("h-1");
        habit.setScheduleDays(mask);
        habit.setStartDate(start);
        habit.setTargetCount(1);
        return habit;
    }
}
