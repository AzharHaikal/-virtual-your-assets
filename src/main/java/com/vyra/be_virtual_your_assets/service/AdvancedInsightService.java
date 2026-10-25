package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.insight.AdvancedInsightResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Advanced, descriptive analytics for the member themself (Phase 3.5). Free for everyone. */
@Service
@RequiredArgsConstructor
public class AdvancedInsightService {

    static final int WINDOW_DAYS = 90;
    static final int MIN_DAYS = 5;
    static final int MONTHS = 12;
    static final int MIN_PACE_DAYS = 7;

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final GoalProgressLogRepository progressLogRepository;
    private final ValidationService validationService;
    private final TimeService timeService;

    @Transactional(readOnly = true)
    public BaseResponse<AdvancedInsightResponse> get(String memberId) {
        LocalDate today = timeService.today(timeService.zoneOf(validationService.getMemberById(memberId)));
        LocalDate from = today.minusDays(WINDOW_DAYS - 1L);

        AdvancedInsightResponse response = new AdvancedInsightResponse();
        response.setFrom(from);
        response.setTo(today);
        response.setHabitMood(habitMood(memberId, from, today));
        response.setMonths(months(memberId, YearMonth.from(today)));
        response.setGoals(goals(memberId, today));
        return new BaseResponse<>(ErrorConstant.INSIGHT_ADVANCED_SUCCESS.getCode(),
                ErrorConstant.INSIGHT_ADVANCED_SUCCESS.getMessage(), response);
    }

    /** Average mood on days each habit was done vs. scheduled but missed (today counts only when done). */
    List<AdvancedInsightResponse.HabitMood> habitMood(String memberId, LocalDate from, LocalDate today) {
        Map<LocalDate, Short> moods = new HashMap<>();
        for (MoodEntry entry : moodEntryRepository.findRange(memberId, from, today)) {
            moods.put(entry.getLocalDate(), entry.getMoodScore());
        }
        if (moods.isEmpty()) {
            return List.of();
        }
        Map<String, Set<LocalDate>> doneDays = new HashMap<>();
        for (Object[] row : completionRepository.findDoneDays(memberId, from, today)) {
            doneDays.computeIfAbsent((String) row[0], k -> new HashSet<>()).add((LocalDate) row[1]);
        }

        List<AdvancedInsightResponse.HabitMood> result = new ArrayList<>();
        for (Habit habit : habitRepository.findAllOfMember(memberId)) {
            if (habit.isArchived()) {
                continue;
            }
            Set<LocalDate> done = doneDays.getOrDefault(habit.getId(), Set.of());
            List<Short> whenDone = new ArrayList<>();
            List<Short> whenMissed = new ArrayList<>();
            moods.forEach((day, score) -> {
                if (!habit.isScheduledOn(day)) {
                    return;
                }
                if (done.contains(day)) {
                    whenDone.add(score);
                } else if (!day.equals(today)) {
                    whenMissed.add(score);
                }
            });
            if (whenDone.size() < MIN_DAYS || whenMissed.size() < MIN_DAYS) {
                continue;
            }
            BigDecimal moodDone = MoodService.average(whenDone);
            BigDecimal moodMissed = MoodService.average(whenMissed);
            result.add(new AdvancedInsightResponse.HabitMood(habit.getId(), habit.getName(), habit.getCategory(),
                    whenDone.size(), whenMissed.size(), moodDone, moodMissed, moodDone.subtract(moodMissed)));
        }
        result.sort(Comparator.comparing((AdvancedInsightResponse.HabitMood h) -> h.getDifference().abs()).reversed());
        return result;
    }

    /** The last 12 calendar months, oldest first, the current one included. */
    List<AdvancedInsightResponse.Month> months(String memberId, YearMonth current) {
        List<AdvancedInsightResponse.Month> result = new ArrayList<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Object[] row : transactionRepository.sumByCategory(memberId, month.atDay(1), month.atEndOfMonth())) {
                BigDecimal amount = new BigDecimal(String.valueOf(row[2])).abs();
                if (TransactionType.INCOME.name().equals(String.valueOf(row[1]))) {
                    income = income.add(amount);
                } else if (TransactionType.EXPENSE.name().equals(String.valueOf(row[1]))) {
                    expense = expense.add(amount);
                }
            }
            result.add(new AdvancedInsightResponse.Month(month.toString(), income, expense, income.subtract(expense)));
        }
        return result;
    }

    /** Active value goals with a target, and when they are reached at the pace of the last 90 days. */
    List<AdvancedInsightResponse.GoalProjection> goals(String memberId, LocalDate today) {
        List<Goal> goals = goalRepository.findAllOfMember(memberId).stream()
                .filter(g -> g.getStatus() == GoalStatus.ACTIVE && g.getProgressType() == GoalProgressType.VALUE)
                .filter(g -> g.getTargetValue() != null && g.getTargetValue().signum() > 0)
                .toList();
        if (goals.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = timeService.nowUtc();
        Map<String, List<GoalProgressLog>> logs = progressLogRepository
                .findByGoalIdInOrderByCreatedAtAsc(goals.stream().map(Goal::getId).toList()).stream()
                .collect(Collectors.groupingBy(GoalProgressLog::getGoalId));

        List<AdvancedInsightResponse.GoalProjection> result = new ArrayList<>();
        for (Goal goal : goals) {
            LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);
            if (goal.getCreatedAt() != null && goal.getCreatedAt().isAfter(windowStart)) {
                windowStart = goal.getCreatedAt();
            }
            LocalDateTime start = windowStart;
            long days = Math.max(MIN_PACE_DAYS, ChronoUnit.DAYS.between(start, now));
            BigDecimal added = logs.getOrDefault(goal.getId(), List.of()).stream()
                    .filter(l -> !l.getCreatedAt().isBefore(start))
                    .map(GoalProgressLog::getDelta)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pace = added.multiply(BigDecimal.valueOf(30)).divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
            result.add(new AdvancedInsightResponse.GoalProjection(goal.getId(), goal.getTitle(), goal.isLinkedToFinance(),
                    goal.getTargetValue(), goal.getCurrentValue(), goal.getUnit(), pace,
                    projectedDate(goal, pace, today), goal.getDueDate(), null));
        }
        result.forEach(p -> p.setOnTrack(p.getDueDate() == null ? null
                : p.getProjectedDate() != null && !p.getProjectedDate().isAfter(p.getDueDate())));
        return result;
    }

    static LocalDate projectedDate(Goal goal, BigDecimal pacePerMonth, LocalDate today) {
        BigDecimal remaining = goal.getTargetValue().subtract(goal.getCurrentValue());
        if (remaining.signum() <= 0) {
            return today;
        }
        if (pacePerMonth.signum() <= 0) {
            return null;
        }
        long days = remaining.multiply(BigDecimal.valueOf(30)).divide(pacePerMonth, 0, RoundingMode.CEILING).longValue();
        return days > 365L * 50 ? null : today.plusDays(days);
    }
}
