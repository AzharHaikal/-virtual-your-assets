package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.entity.Budget;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Notification;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRelevanceTest {

    /** 2026-10-22 05:00 UTC = 12:00 WIB on the 22nd. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private BudgetRepository budgetRepository;

    private NotificationRelevance relevance;
    private final Member member = new Member();

    @BeforeEach
    void setUp() {
        relevance = new NotificationRelevance(habitRepository, completionRepository, goalRepository, budgetRepository,
                new TimeService(Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
        member.setId("m-1");
        member.setTimezone("Asia/Jakarta");
    }

    @Test
    void habitReminder_relevantOnlyTodayWhileNotDone() {
        Habit habit = new Habit();
        habit.setId("h-1");
        habit.setTargetCount(2);
        habit.setReminderEnabled(true);
        habit.setReminderTime(LocalTime.of(7, 0));
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", "m-1")).thenReturn(Optional.of(habit));

        assertThat(relevance.isRelevant(habitReminder(TODAY), member)).isTrue();

        HabitCompletion one = new HabitCompletion();
        one.setCount(2);
        when(completionRepository.findByHabitIdAndLocalDate("h-1", TODAY)).thenReturn(Optional.of(one));
        assertThat(relevance.isRelevant(habitReminder(TODAY), member)).isFalse();

        // Yesterday's reminder held back by quiet hours is not pushed today.
        assertThat(relevance.isRelevant(habitReminder(TODAY.minusDays(1)), member)).isFalse();
    }

    @Test
    void goalAndBudget_relevantWhileTheyStillExist() {
        Goal goal = new Goal();
        goal.setStatus(GoalStatus.ACTIVE);
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-1")).thenReturn(Optional.of(goal));
        assertThat(relevance.isRelevant(notification(NotificationType.GOAL_DUE, Map.of("goalId", "g-1")), member)).isTrue();
        goal.setStatus(GoalStatus.COMPLETED);
        assertThat(relevance.isRelevant(notification(NotificationType.GOAL_DUE, Map.of("goalId", "g-1")), member)).isFalse();

        when(budgetRepository.findByIdAndMemberIdAndDeletedAtIsNull("b-1", "m-1")).thenReturn(Optional.of(new Budget()));
        assertThat(relevance.isRelevant(notification(NotificationType.BUDGET_ALERT, Map.of("budgetId", "b-1")), member)).isTrue();
        assertThat(relevance.isRelevant(notification(NotificationType.BUDGET_ALERT, Map.of("budgetId", "b-gone")), member)).isFalse();

        assertThat(relevance.isRelevant(notification(NotificationType.SYSTEM, null), member)).isTrue();
    }

    private static Notification habitReminder(LocalDate day) {
        return notification(NotificationType.HABIT_REMINDER, Map.of("habitId", "h-1", "localDate", day.toString()));
    }

    private static Notification notification(NotificationType type, Map<String, String> payload) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setPayload(payload);
        return notification;
    }
}
