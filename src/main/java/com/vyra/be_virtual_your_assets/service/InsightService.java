package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.insight.WeeklyInsightResponse;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Weekly summary across modules: habits done, average mood and spending against the week before. */
@Service
@RequiredArgsConstructor
public class InsightService {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final TransactionRepository transactionRepository;
    private final ValidationService validationService;
    private final TimeService timeService;

    /** The calendar week containing {@code date} (default today, member's zone). */
    @Transactional(readOnly = true)
    public BaseResponse<WeeklyInsightResponse> getWeekly(String memberId, LocalDate date) {
        LocalDate today = timeService.today(timeService.zoneOf(validationService.getMemberById(memberId)));
        LocalDate anchor = date != null ? date : today;
        LocalDate monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        WeeklyInsightResponse response = new WeeklyInsightResponse();
        response.setFrom(monday);
        response.setTo(sunday);
        response.setHabits(habits(memberId, monday, sunday, today));
        response.setMood(mood(memberId, monday, sunday));
        response.setExpense(expense(memberId, monday, sunday));
        return new BaseResponse<>(ErrorConstant.INSIGHT_WEEKLY_SUCCESS.getCode(),
                ErrorConstant.INSIGHT_WEEKLY_SUCCESS.getMessage(), response);
    }

    private WeeklyInsightResponse.Habits habits(String memberId, LocalDate monday, LocalDate sunday, LocalDate today) {
        Map<String, Map<LocalDate, Integer>> counts = completionRepository.findRange(memberId, monday, sunday).stream()
                .collect(Collectors.groupingBy(HabitCompletion::getHabitId,
                        Collectors.toMap(HabitCompletion::getLocalDate, HabitCompletion::getCount)));
        int scheduled = 0;
        int done = 0;
        for (Habit habit : habitRepository.findAllOfMember(memberId)) {
            if (habit.isArchived()) {
                continue;
            }
            Map<LocalDate, Integer> byDay = counts.getOrDefault(habit.getId(), Map.of());
            for (LocalDate day = monday; !day.isAfter(sunday) && !day.isAfter(today); day = day.plusDays(1)) {
                if (!habit.isScheduledOn(day)) {
                    continue;
                }
                boolean isDone = byDay.getOrDefault(day, 0) >= habit.getTargetCount();
                if (day.equals(today) && !isDone) {
                    continue; // not missed yet
                }
                scheduled++;
                if (isDone) {
                    done++;
                }
            }
        }
        WeeklyInsightResponse.Habits habits = new WeeklyInsightResponse.Habits();
        habits.setScheduled(scheduled);
        habits.setDone(done);
        habits.setPercent(scheduled == 0 ? null : done * 100 / scheduled);
        return habits;
    }

    private WeeklyInsightResponse.Mood mood(String memberId, LocalDate monday, LocalDate sunday) {
        List<MoodEntry> entries = moodEntryRepository.findRange(memberId, monday, sunday);
        WeeklyInsightResponse.Mood mood = new WeeklyInsightResponse.Mood();
        mood.setEntries(entries.size());
        mood.setAverage(MoodService.average(entries.stream().map(MoodEntry::getMoodScore).toList()));
        return mood;
    }

    private WeeklyInsightResponse.Expense expense(String memberId, LocalDate monday, LocalDate sunday) {
        BigDecimal thisWeek = expenses(memberId, monday, sunday);
        BigDecimal lastWeek = expenses(memberId, monday.minusDays(7), sunday.minusDays(7));
        WeeklyInsightResponse.Expense expense = new WeeklyInsightResponse.Expense();
        expense.setThisWeek(thisWeek);
        expense.setLastWeek(lastWeek);
        expense.setChangePercent(lastWeek.signum() == 0 ? null
                : thisWeek.subtract(lastWeek).multiply(BigDecimal.valueOf(100)).divide(lastWeek, 1, RoundingMode.HALF_UP));
        return expense;
    }

    /** SUCCESS expenses in [from, to], deleted ones excluded, as a positive amount. */
    private BigDecimal expenses(String memberId, LocalDate from, LocalDate to) {
        return transactionRepository.sumByCategory(memberId, from, to).stream()
                .filter(r -> TransactionType.EXPENSE.name().equals(String.valueOf(r[1])))
                .map(r -> new BigDecimal(String.valueOf(r[2])).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
