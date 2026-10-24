package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.entity.Habit;

import java.time.LocalDate;
import java.util.Set;

/**
 * Streaks of a habit, counted over its <b>scheduled</b> days only (member's local days):
 * <ul>
 *   <li>a day the habit is not scheduled on is skipped and never breaks a streak;</li>
 *   <li>today not done (yet) does not break the streak either, it just does not add to it;</li>
 *   <li>any other scheduled day not done (including partly done) ends the streak.</li>
 * </ul>
 * Only days from {@code from} on are looked at, so a streak longer than the look-back stops there.
 */
final class HabitStreakCalculator {

    record Streak(int current, int best) {
    }

    private HabitStreakCalculator() {
    }

    static Streak compute(Habit habit, Set<LocalDate> doneDays, LocalDate today, LocalDate from) {
        LocalDate start = from.isAfter(habit.getStartDate()) ? from : habit.getStartDate();
        int best = 0;
        int run = 0;
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            if (!habit.isScheduledOn(day)) {
                continue;
            }
            if (doneDays.contains(day)) {
                run++;
                best = Math.max(best, run);
            } else if (!day.equals(today)) {
                run = 0;
            }
        }
        // The run that reaches today is the current streak.
        return new Streak(run, best);
    }
}
