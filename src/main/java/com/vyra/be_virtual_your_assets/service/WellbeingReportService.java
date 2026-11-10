package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.GoalReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.HabitReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.MoodReportItemResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.GoalMilestoneRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Member-level wellbeing reports for the portal: mood check-ins, habits and goals.
 * <p>
 * SUPER_ADMIN only ({@link AdminPermission#MEMBER_WELLBEING_VIEW}); every other role gets 403.
 * The Super Admin sees everything, including the mood note (the member's journal, decrypted from
 * its encrypted column). Opening a report is written to the audit trail.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WellbeingReportService {

    private static final int MAX_PAGE_SIZE = 500;

    private final MoodEntryRepository moodEntryRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final GoalRepository goalRepository;
    private final GoalMilestoneRepository goalMilestoneRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;
    private final TimeService timeService;

    // ─── Mood ────────────────────────────────────────────────────────────────

    /** Every check-in in [startDate, endDate] (members' local days), newest first. */
    public BaseResponse<PageResponse<MoodReportItemResponse>> getMoodReport(
            LocalDate startDate, LocalDate endDate, String keyword, int page, int size) {
        requireAccess(page, size, "mood report " + startDate + " to " + endDate);
        if (startDate.isAfter(endDate)) {
            return ok(ErrorConstant.REPORT_MOOD_SUCCESS, emptyPage(page, size), List.of());
        }

        Page<Object[]> rows = moodEntryRepository.findForReport(startDate, endDate, pattern(keyword), pageable(page, size));
        List<String> ids = rows.getContent().stream().map(r -> ((MoodEntry) r[0]).getId()).toList();
        Map<String, List<String>> tags = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : moodEntryRepository.findTagNames(ids)) {
                tags.computeIfAbsent((String) row[0], k -> new ArrayList<>()).add((String) row[1]);
            }
        }

        List<MoodReportItemResponse> items = map(rows, (entry, member) -> {
            MoodEntry e = (MoodEntry) entry;
            MoodReportItemResponse item = new MoodReportItemResponse();
            item.setEntryId(e.getId());
            fillMember(member, item::setMemberName, item::setMemberEmail, item::setMemberPhoneNumber);
            item.setDate(e.getLocalDate());
            item.setMoodScore(e.getMoodScore());
            item.setTags(tags.getOrDefault(e.getId(), List.of()));
            item.setNote(e.getNote());
            item.setCheckedInAt(e.getCheckedInAt());
            return item;
        });
        return ok(ErrorConstant.REPORT_MOOD_SUCCESS, rows, items);
    }

    // ─── Habits ──────────────────────────────────────────────────────────────

    /** Every habit started by endDate, with how often it was done in [startDate, endDate]. */
    public BaseResponse<PageResponse<HabitReportItemResponse>> getHabitReport(
            LocalDate startDate, LocalDate endDate, String keyword, int page, int size) {
        requireAccess(page, size, "habit report " + startDate + " to " + endDate);
        if (startDate.isAfter(endDate)) {
            return ok(ErrorConstant.REPORT_HABIT_SUCCESS, emptyPage(page, size), List.of());
        }

        Page<Object[]> rows = habitRepository.findForReport(endDate, pattern(keyword), pageable(page, size));
        List<String> ids = rows.getContent().stream().map(r -> ((Habit) r[0]).getId()).toList();
        Map<String, Object[]> summaries = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : habitCompletionRepository.summarizeForReport(ids, startDate, endDate)) {
                summaries.put((String) row[0], row);
            }
        }

        List<HabitReportItemResponse> items = map(rows, (entity, memberEntity) -> {
            Habit habit = (Habit) entity;
            Member member = (Member) memberEntity;
            Object[] summary = summaries.get(habit.getId());
            int completed = summary == null || summary[1] == null ? 0 : ((Number) summary[1]).intValue();
            int scheduled = scheduledDays(habit, member, startDate, endDate);

            HabitReportItemResponse item = new HabitReportItemResponse();
            item.setHabitId(habit.getId());
            fillMember(member, item::setMemberName, item::setMemberEmail, item::setMemberPhoneNumber);
            item.setName(habit.getName());
            item.setCategory(habit.getCategory());
            item.setTargetCount(habit.getTargetCount());
            item.setScheduleDays(habit.scheduledDays());
            item.setStartDate(habit.getStartDate());
            item.setArchived(habit.isArchived());
            item.setScheduledDays(scheduled);
            item.setCompletedDays(completed);
            item.setCompletionRate(scheduled == 0 ? 0 : Math.min(100, completed * 100 / scheduled));
            item.setLastActivityDate(summary == null ? null : (LocalDate) summary[2]);
            return item;
        });
        return ok(ErrorConstant.REPORT_HABIT_SUCCESS, rows, items);
    }

    /**
     * Days in [startDate, endDate] the habit was scheduled on: from its start date, never after the
     * member's today, and before the day it was archived.
     */
    int scheduledDays(Habit habit, Member member, LocalDate startDate, LocalDate endDate) {
        ZoneId zone = timeService.zoneOf(member);
        LocalDate last = endDate;
        LocalDate today = timeService.today(zone);
        if (today.isBefore(last)) {
            last = today;
        }
        if (habit.getArchivedAt() != null) {
            LocalDate archivedOn = timeService.toLocalDate(habit.getArchivedAt(), zone);
            if (!archivedOn.isAfter(last)) {
                last = archivedOn.minusDays(1);
            }
        }
        int days = 0;
        for (LocalDate day = startDate; !day.isAfter(last); day = day.plusDays(1)) {
            if (habit.isScheduledOn(day)) {
                days++;
            }
        }
        return days;
    }

    // ─── Goals ───────────────────────────────────────────────────────────────

    /** Every goal (optionally of one status), newest first, with its progress. */
    public BaseResponse<PageResponse<GoalReportItemResponse>> getGoalReport(
            GoalStatus status, String keyword, int page, int size) {
        requireAccess(page, size, "goal report" + (status != null ? " " + status : ""));

        List<GoalStatus> statuses = status != null ? List.of(status) : Arrays.asList(GoalStatus.values());
        Page<Object[]> rows = goalRepository.findForReport(statuses, pattern(keyword), pageable(page, size));
        List<String> ids = rows.getContent().stream().map(r -> ((Goal) r[0]).getId()).toList();
        Map<String, int[]> milestones = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : goalMilestoneRepository.countByGoals(ids)) {
                milestones.put((String) row[0], new int[]{
                        ((Number) row[1]).intValue(),
                        row[2] == null ? 0 : ((Number) row[2]).intValue()});
            }
        }

        List<GoalReportItemResponse> items = map(rows, (entity, member) -> {
            Goal goal = (Goal) entity;
            int[] counts = milestones.getOrDefault(goal.getId(), new int[]{0, 0});

            GoalReportItemResponse item = new GoalReportItemResponse();
            item.setGoalId(goal.getId());
            fillMember(member, item::setMemberName, item::setMemberEmail, item::setMemberPhoneNumber);
            item.setTitle(goal.getTitle());
            item.setCategory(goal.getCategory());
            item.setProgressType(goal.getProgressType());
            item.setCurrentValue(goal.getCurrentValue());
            item.setTargetValue(goal.getTargetValue());
            item.setUnit(goal.getUnit());
            item.setMilestonesTotal(counts[0]);
            item.setMilestonesDone(counts[1]);
            item.setProgressPercent(GoalService.progressPercent(goal, counts[1], counts[0]));
            item.setStatus(goal.getStatus());
            item.setDueDate(goal.getDueDate());
            item.setCreatedAt(goal.getCreatedAt());
            item.setCompletedAt(goal.getCompletedAt());
            return item;
        });
        return ok(ErrorConstant.REPORT_GOAL_SUCCESS, rows, items);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** SUPER_ADMIN only; the first page of every report opened is written to the audit trail. */
    private void requireAccess(int page, int size, String description) {
        adminAuthorizationService.requirePermission(AdminPermission.MEMBER_WELLBEING_VIEW);
        adminAuthorizationService.requireExportIfBulk(size);
        if (page <= 0) {
            adminActivityService.createAdminActivity(adminAuthorizationService.getCurrentAdmin().getAdminId(), null,
                    AdminActivityEvent.VIEW_MEMBER_WELLBEING, description, null, null);
        }
    }

    private static String pattern(String keyword) {
        return StringUtils.isBlank(keyword) ? "%" : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private static Page<Object[]> emptyPage(int page, int size) {
        return new PageImpl<>(List.of(), pageable(page, size), 0);
    }

    private interface RowMapper<T> {
        T map(Object entity, Object member);
    }

    private static <T> List<T> map(Page<Object[]> rows, RowMapper<T> mapper) {
        return rows.getContent().stream().map(r -> mapper.map(r[0], r[1])).toList();
    }

    private static void fillMember(Object entity, Consumer<String> name, Consumer<String> email, Consumer<String> phone) {
        Member member = (Member) entity;
        name.accept((StringUtils.defaultString(member.getFirstName()) + " "
                + StringUtils.defaultString(member.getLastName())).trim());
        email.accept(member.getEmail());
        phone.accept(member.getPhoneNumber());
    }

    private static <T> BaseResponse<PageResponse<T>> ok(ErrorConstant code, Page<?> page, List<T> items) {
        return new BaseResponse<>(code.getCode(), code.getMessage(), PageResponse.from(page, items));
    }
}
