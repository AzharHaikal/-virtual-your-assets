package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitDayStatus;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCalendarDayResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCalendarResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCompletionRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitDayResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.habit.UpdateHabitRequest;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Habit tracker. Days are the member's local days. A day the habit is not scheduled on never counts
 * as missed; weekly progress = scheduled days done / scheduled days of the week (Monday .. Sunday).
 */
@Service
@RequiredArgsConstructor
public class HabitService {

    static final int MAX_HABITS = 50;
    /** Today and the 6 days before it can be marked. */
    static final int EDITABLE_DAYS = 7;
    /** Streaks look this many days back at most. */
    static final int STREAK_LOOKBACK_DAYS = 400;

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final ValidationService validationService;
    private final TimeService timeService;
    private final ActivationService activationService;

    // =========================================================================
    // Read
    // =========================================================================

    /** All habits (optionally one category), active ones by default or only the archived ones. */
    @Transactional(readOnly = true)
    public BaseResponse<List<HabitResponse>> getHabits(String memberId, HabitCategory category, boolean archived) {
        List<Habit> habits = habitRepository.findAllOfMember(memberId).stream()
                .filter(h -> h.isArchived() == archived)
                .filter(h -> category == null || h.getCategory() == category)
                .toList();
        return new BaseResponse<>(ErrorConstant.HABIT_LIST_SUCCESS.getCode(),
                ErrorConstant.HABIT_LIST_SUCCESS.getMessage(), toResponses(memberId, habits, today(memberId)));
    }

    /** Today's checklist: active habits scheduled today. */
    @Transactional(readOnly = true)
    public BaseResponse<List<HabitResponse>> getToday(String memberId) {
        LocalDate today = today(memberId);
        List<Habit> habits = habitRepository.findAllOfMember(memberId).stream()
                .filter(h -> !h.isArchived() && h.isScheduledOn(today))
                .toList();
        return new BaseResponse<>(ErrorConstant.HABIT_TODAY_SUCCESS.getCode(),
                ErrorConstant.HABIT_TODAY_SUCCESS.getMessage(), toResponses(memberId, habits, today));
    }

    /** One month (default: the current one) of a habit, day by day. */
    @Transactional(readOnly = true)
    public BaseResponse<HabitCalendarResponse> getCalendar(String memberId, String habitId, YearMonth month) {
        Habit habit = findOwned(memberId, habitId);
        LocalDate today = today(memberId);
        YearMonth period = month != null ? month : YearMonth.from(today);
        Map<LocalDate, Integer> counts = completionRepository
                .findByHabitIdAndLocalDateBetween(habit.getId(), period.atDay(1), period.atEndOfMonth()).stream()
                .collect(Collectors.toMap(HabitCompletion::getLocalDate, HabitCompletion::getCount));

        List<HabitCalendarDayResponse> days = new ArrayList<>(period.lengthOfMonth());
        int scheduled = 0;
        int done = 0;
        for (LocalDate day = period.atDay(1); !day.isAfter(period.atEndOfMonth()); day = day.plusDays(1)) {
            int count = counts.getOrDefault(day, 0);
            HabitDayStatus status = dayStatus(habit, day, today, count);
            if (status == HabitDayStatus.DONE || status == HabitDayStatus.PARTIAL || status == HabitDayStatus.MISSED) {
                scheduled++;
            }
            if (status == HabitDayStatus.DONE) {
                done++;
            }
            days.add(new HabitCalendarDayResponse(day, status, count));
        }
        HabitCalendarResponse response = new HabitCalendarResponse();
        response.setHabitId(habit.getId());
        response.setMonth(period);
        response.setTargetCount(habit.getTargetCount());
        response.setScheduledDays(scheduled);
        response.setDoneDays(done);
        response.setDays(days);
        return new BaseResponse<>(ErrorConstant.HABIT_CALENDAR_SUCCESS.getCode(),
                ErrorConstant.HABIT_CALENDAR_SUCCESS.getMessage(), response);
    }

    /** A done day is DONE even if the schedule changed since; days before the start or after today are out of range. */
    static HabitDayStatus dayStatus(Habit habit, LocalDate day, LocalDate today, int count) {
        if (day.isAfter(today) || day.isBefore(habit.getStartDate())) {
            return HabitDayStatus.OUT_OF_RANGE;
        }
        if (count >= habit.getTargetCount()) {
            return HabitDayStatus.DONE;
        }
        if (!habit.isScheduledOn(day)) {
            return HabitDayStatus.NOT_SCHEDULED;
        }
        if (day.equals(today)) {
            return HabitDayStatus.PENDING;
        }
        return count > 0 ? HabitDayStatus.PARTIAL : HabitDayStatus.MISSED;
    }

    // =========================================================================
    // Write
    // =========================================================================

    @Transactional
    public BaseResponse<HabitResponse> createHabit(String memberId, CreateHabitRequest request) {
        if (habitRepository.countByMemberIdAndDeletedAtIsNull(memberId) >= MAX_HABITS) {
            throw new BadRequestException(ErrorConstant.HABIT_LIMIT_REACHED);
        }
        LocalDate today = today(memberId);
        Habit habit = new Habit();
        habit.setMemberId(memberId);
        habit.setName(request.getName().trim());
        habit.setCategory(request.getCategory());
        habit.setIcon(request.getIcon());
        habit.setColor(request.getColor().toUpperCase());
        habit.setTargetCount(request.getTargetCount() != null ? request.getTargetCount() : 1);
        habit.setScheduleDays(scheduleMask(request.getScheduleType(), request.getScheduleDays()));
        habit.setStartDate(today);
        habit.setCreatedBy(memberId);
        habit.setCreatedAt(timeService.nowUtc());
        habitRepository.save(habit);
        return new BaseResponse<>(ErrorConstant.HABIT_CREATE_SUCCESS.getCode(),
                ErrorConstant.HABIT_CREATE_SUCCESS.getMessage(), toResponse(memberId, habit, today));
    }

    @Transactional
    public BaseResponse<HabitResponse> updateHabit(String memberId, String habitId, UpdateHabitRequest request) {
        Habit habit = findOwned(memberId, habitId);
        if (request.getName() != null) {
            habit.setName(request.getName().trim());
        }
        if (request.getCategory() != null) {
            habit.setCategory(request.getCategory());
        }
        if (request.getIcon() != null) {
            habit.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            habit.setColor(request.getColor().toUpperCase());
        }
        if (request.getTargetCount() != null) {
            habit.setTargetCount(request.getTargetCount());
        }
        if (request.getScheduleType() != null) {
            habit.setScheduleDays(scheduleMask(request.getScheduleType(), request.getScheduleDays()));
        } else if (request.getScheduleDays() != null) {
            habit.setScheduleDays(scheduleMask(HabitScheduleType.SPECIFIC_DAYS, request.getScheduleDays()));
        }
        touch(habit, memberId);
        habitRepository.save(habit);
        return new BaseResponse<>(ErrorConstant.HABIT_UPDATE_SUCCESS.getCode(),
                ErrorConstant.HABIT_UPDATE_SUCCESS.getMessage(), toResponse(memberId, habit, today(memberId)));
    }

    /** Soft delete; the completions stay in the database. */
    @Transactional
    public BaseResponse<Void> deleteHabit(String memberId, String habitId) {
        Habit habit = findOwned(memberId, habitId);
        habit.setDeletedAt(timeService.nowUtc());
        habit.setModifiedBy(memberId);
        habitRepository.save(habit);
        return new BaseResponse<>(ErrorConstant.HABIT_DELETE_SUCCESS.getCode(),
                ErrorConstant.HABIT_DELETE_SUCCESS.getMessage(), null);
    }

    /** Archived habits leave the checklist but keep their history. Idempotent both ways. */
    @Transactional
    public BaseResponse<HabitResponse> setArchived(String memberId, String habitId, boolean archived) {
        Habit habit = findOwned(memberId, habitId);
        if (archived != habit.isArchived()) {
            habit.setArchivedAt(archived ? timeService.nowUtc() : null);
            touch(habit, memberId);
            habitRepository.save(habit);
        }
        ErrorConstant status = archived ? ErrorConstant.HABIT_ARCHIVE_SUCCESS : ErrorConstant.HABIT_UNARCHIVE_SUCCESS;
        return new BaseResponse<>(status.getCode(), status.getMessage(), toResponse(memberId, habit, today(memberId)));
    }

    /** Sets the progress of a day (idempotent); without a count the habit is marked fully done. */
    @Transactional
    public BaseResponse<HabitResponse> complete(String memberId, String habitId, LocalDate localDate,
                                                HabitCompletionRequest request) {
        Habit habit = findOwned(memberId, habitId);
        LocalDate today = today(memberId);
        checkMarkable(habit, localDate, today);
        int count = request != null && request.getCount() != null ? request.getCount() : habit.getTargetCount();
        completionRepository.upsert(UUID.randomUUID().toString(), habit.getId(), memberId, localDate, count,
                timeService.nowUtc());
        activationService.record(memberId, ActivationEvent.FIRST_HABIT_COMPLETION);
        return new BaseResponse<>(ErrorConstant.HABIT_COMPLETION_SUCCESS.getCode(),
                ErrorConstant.HABIT_COMPLETION_SUCCESS.getMessage(), toResponse(memberId, habit, today));
    }

    /** Removes the progress of a day (idempotent). */
    @Transactional
    public BaseResponse<HabitResponse> uncomplete(String memberId, String habitId, LocalDate localDate) {
        Habit habit = findOwned(memberId, habitId);
        LocalDate today = today(memberId);
        checkDateWindow(localDate, today);
        completionRepository.deleteDay(habit.getId(), localDate);
        return new BaseResponse<>(ErrorConstant.HABIT_COMPLETION_DELETE_SUCCESS.getCode(),
                ErrorConstant.HABIT_COMPLETION_DELETE_SUCCESS.getMessage(), toResponse(memberId, habit, today));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void checkMarkable(Habit habit, LocalDate day, LocalDate today) {
        if (habit.isArchived()) {
            throw new BadRequestException(ErrorConstant.HABIT_ARCHIVED);
        }
        checkDateWindow(day, today);
        if (!habit.isScheduledOn(day)) {
            throw new BadRequestException(ErrorConstant.HABIT_NOT_SCHEDULED);
        }
    }

    private static void checkDateWindow(LocalDate day, LocalDate today) {
        if (day.isAfter(today)) {
            throw new BadRequestException(ErrorConstant.HABIT_DATE_IN_FUTURE);
        }
        if (day.isBefore(today.minusDays(EDITABLE_DAYS - 1L))) {
            throw new BadRequestException(ErrorConstant.HABIT_DATE_TOO_OLD);
        }
    }

    private static short scheduleMask(HabitScheduleType type, List<DayOfWeek> days) {
        if (type == HabitScheduleType.DAILY) {
            return Habit.EVERY_DAY;
        }
        if (days == null || days.isEmpty()) {
            throw new BadRequestException(ErrorConstant.HABIT_SCHEDULE_DAYS_REQUIRED);
        }
        return Habit.mask(days);
    }

    private Habit findOwned(String memberId, String habitId) {
        return habitRepository.findByIdAndMemberIdAndDeletedAtIsNull(habitId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.HABIT_NOT_FOUND));
    }

    private void touch(Habit habit, String memberId) {
        habit.setModifiedBy(memberId);
        habit.setUpdatedAt(timeService.nowUtc());
    }

    private LocalDate today(String memberId) {
        return timeService.today(timeService.zoneOf(validationService.getMemberById(memberId)));
    }

    private HabitResponse toResponse(String memberId, Habit habit, LocalDate today) {
        return toResponses(memberId, List.of(habit), today).get(0);
    }

    /** Responses with this week's progress; one query for the completions of all habits. */
    private List<HabitResponse> toResponses(String memberId, List<Habit> habits, LocalDate today) {
        if (habits.isEmpty()) {
            return List.of();
        }
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        Map<String, Map<LocalDate, Integer>> counts = completionRepository.findRange(memberId, monday, sunday).stream()
                .collect(Collectors.groupingBy(HabitCompletion::getHabitId,
                        Collectors.toMap(HabitCompletion::getLocalDate, HabitCompletion::getCount)));
        LocalDate streakFrom = today.minusDays(STREAK_LOOKBACK_DAYS);
        Map<String, Set<LocalDate>> doneDays = new HashMap<>();
        for (Object[] row : completionRepository.findDoneDays(memberId, streakFrom, today)) {
            doneDays.computeIfAbsent((String) row[0], id -> new HashSet<>()).add((LocalDate) row[1]);
        }

        return habits.stream().map(habit -> {
            Map<LocalDate, Integer> byDay = counts.getOrDefault(habit.getId(), Map.of());
            List<HabitDayResponse> week = new ArrayList<>(7);
            int weekDone = 0;
            int weekScheduled = 0;
            for (LocalDate day = monday; !day.isAfter(sunday); day = day.plusDays(1)) {
                boolean scheduled = habit.isScheduledOn(day);
                int count = byDay.getOrDefault(day, 0);
                boolean done = count >= habit.getTargetCount();
                if (scheduled) {
                    weekScheduled++;
                    if (done) {
                        weekDone++;
                    }
                }
                week.add(new HabitDayResponse(day, scheduled, count, done));
            }
            HabitResponse response = new HabitResponse();
            response.setId(habit.getId());
            response.setName(habit.getName());
            response.setCategory(habit.getCategory());
            response.setIcon(habit.getIcon());
            response.setColor(habit.getColor());
            response.setTargetCount(habit.getTargetCount());
            response.setScheduleType(habit.getScheduleDays() == Habit.EVERY_DAY
                    ? HabitScheduleType.DAILY : HabitScheduleType.SPECIFIC_DAYS);
            response.setScheduleDays(habit.scheduledDays());
            response.setStartDate(habit.getStartDate());
            response.setArchived(habit.isArchived());
            response.setScheduledToday(habit.isScheduledOn(today));
            response.setTodayCount(byDay.getOrDefault(today, 0));
            response.setDoneToday(response.getTodayCount() >= habit.getTargetCount());
            response.setWeekDone(weekDone);
            response.setWeekScheduled(weekScheduled);
            response.setWeek(week);
            HabitStreakCalculator.Streak streak = HabitStreakCalculator.compute(
                    habit, doneDays.getOrDefault(habit.getId(), Set.of()), today, streakFrom);
            response.setCurrentStreak(streak.current());
            response.setBestStreak(streak.best());
            return response;
        }).toList();
    }
}
