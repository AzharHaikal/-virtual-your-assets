package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Notification;
import com.vyra.be_virtual_your_assets.repository.AnnouncementRepository;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Whether a notification held back (quiet hours, retry) is still worth pushing: a habit reminder of a day
 * that has passed or of a habit that is done, archived or deleted is not; neither is a goal no longer active
 * or a budget that was deleted, nor an announcement that was unpublished, deleted or has ended.
 */
@Component
@RequiredArgsConstructor
public class NotificationRelevance {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final GoalRepository goalRepository;
    private final BudgetRepository budgetRepository;
    private final AnnouncementRepository announcementRepository;
    private final TimeService timeService;

    public boolean isRelevant(Notification notification, Member member) {
        Map<String, String> payload = notification.getPayload() != null ? notification.getPayload() : Map.of();
        return switch (notification.getType()) {
            case HABIT_REMINDER -> habitReminderRelevant(payload, member);
            case GOAL_DUE -> payload.get("goalId") != null && goalRepository
                    .findByIdAndMemberIdAndDeletedAtIsNull(payload.get("goalId"), member.getId())
                    .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                    .isPresent();
            case BUDGET_ALERT -> payload.get("budgetId") != null && budgetRepository
                    .findByIdAndMemberIdAndDeletedAtIsNull(payload.get("budgetId"), member.getId())
                    .isPresent();
            case ANNOUNCEMENT -> payload.get("announcementId") != null && announcementRepository
                    .findByIdAndDeletedAtIsNull(payload.get("announcementId"))
                    .filter(a -> a.isActiveAt(timeService.nowUtc()))
                    .isPresent();
            case SYSTEM -> true;
        };
    }

    private boolean habitReminderRelevant(Map<String, String> payload, Member member) {
        LocalDate day;
        try {
            day = LocalDate.parse(payload.getOrDefault("localDate", ""));
        } catch (DateTimeParseException e) {
            return false;
        }
        if (!day.equals(timeService.today(timeService.zoneOf(member)))) {
            return false;
        }
        return payload.get("habitId") != null && habitRepository
                .findByIdAndMemberIdAndDeletedAtIsNull(payload.get("habitId"), member.getId())
                .filter(h -> !h.isArchived() && h.isReminderEnabled())
                .filter(h -> completionRepository.findByHabitIdAndLocalDate(h.getId(), day)
                        .map(HabitCompletion::getCount)
                        .filter(count -> count >= h.getTargetCount())
                        .isEmpty())
                .isPresent();
    }
}
