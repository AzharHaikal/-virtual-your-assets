package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.HabitDayStatus;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCalendarResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCompletionRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.habit.UpdateHabitRequest;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-22 17:30 UTC = Friday 2026-10-23 00:30 in Jakarta. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private ValidationService validationService;
    @Mock private ActivationService activationService;

    private HabitService habitService;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        habitService = new HabitService(habitRepository, completionRepository, validationService, timeService,
                activationService);
        Member member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(completionRepository.findRange(eq(MEMBER), any(), any())).thenReturn(List.of());
    }

    // =========================================================================
    // create / update
    // =========================================================================
    @Test
    void createHabit_specificDays_storesMaskAndStartsTodayInTheMembersZone() {
        CreateHabitRequest request = createRequest(HabitScheduleType.SPECIFIC_DAYS,
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        request.setTargetCount(8);

        HabitResponse result = habitService.createHabit(MEMBER, request).getData();

        ArgumentCaptor<Habit> saved = ArgumentCaptor.forClass(Habit.class);
        verify(habitRepository).save(saved.capture());
        assertThat(saved.getValue().getScheduleDays()).isEqualTo((short) 0b0010101);
        assertThat(saved.getValue().getStartDate()).isEqualTo(TODAY);
        assertThat(saved.getValue().getColor()).isEqualTo("#22C55E");
        assertThat(result.getScheduleType()).isEqualTo(HabitScheduleType.SPECIFIC_DAYS);
        assertThat(result.getScheduleDays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertThat(result.isScheduledToday()).isTrue();
        // Started on Friday: only Friday of this week counts.
        assertThat(result.getWeekScheduled()).isEqualTo(1);
    }

    @Test
    void createHabit_specificDaysWithoutDays_orOverTheLimit_isRejected() {
        BadRequestException noDays = assertThrows(BadRequestException.class,
                () -> habitService.createHabit(MEMBER, createRequest(HabitScheduleType.SPECIFIC_DAYS, List.of())));
        assertThat(noDays.getErrorConstant()).isEqualTo(ErrorConstant.HABIT_SCHEDULE_DAYS_REQUIRED);

        when(habitRepository.countByMemberIdAndDeletedAtIsNull(MEMBER)).thenReturn((long) HabitService.MAX_HABITS);
        BadRequestException limit = assertThrows(BadRequestException.class,
                () -> habitService.createHabit(MEMBER, createRequest(HabitScheduleType.DAILY, List.of())));
        assertThat(limit.getErrorConstant()).isEqualTo(ErrorConstant.HABIT_LIMIT_REACHED);
    }

    @Test
    void updateHabit_changesOnlyTheSentFields() {
        Habit habit = habit(Habit.EVERY_DAY, 1);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));
        UpdateHabitRequest request = new UpdateHabitRequest();
        request.setName("  Minum air  ");
        request.setScheduleDays(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        HabitResponse result = habitService.updateHabit(MEMBER, "h-1", request).getData();

        assertThat(habit.getName()).isEqualTo("Minum air");
        assertThat(habit.getCategory()).isEqualTo(HabitCategory.HEALTH);
        assertThat(result.getScheduleDays()).containsExactly(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(result.isScheduledToday()).isFalse();
    }

    @Test
    void anotherMembersHabit_is404() {
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-x", MEMBER)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> habitService.deleteHabit(MEMBER, "h-x"));
        assertThrows(NotFoundException.class, () -> habitService.complete(MEMBER, "h-x", TODAY, null));
    }

    // =========================================================================
    // completions
    // =========================================================================
    @Test
    void complete_withoutCount_marksTheTargetAndIsAnUpsert() {
        Habit habit = habit(Habit.EVERY_DAY, 8);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));

        habitService.complete(MEMBER, "h-1", TODAY, null);

        verify(completionRepository).upsert(anyString(), eq("h-1"), eq(MEMBER), eq(TODAY), eq(8),
                eq(LocalDateTime.of(2026, 10, 22, 17, 30)));
    }

    @Test
    void complete_partialCount_isNotDone() {
        Habit habit = habit(Habit.EVERY_DAY, 8);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));
        when(completionRepository.findRange(MEMBER, MONDAY, MONDAY.plusDays(6)))
                .thenReturn(List.of(completion(TODAY, 3), completion(MONDAY, 8)));
        HabitCompletionRequest request = new HabitCompletionRequest();
        request.setCount(3);

        HabitResponse result = habitService.complete(MEMBER, "h-1", TODAY, request).getData();

        verify(completionRepository).upsert(anyString(), eq("h-1"), eq(MEMBER), eq(TODAY), eq(3), any());
        assertThat(result.getTodayCount()).isEqualTo(3);
        assertThat(result.isDoneToday()).isFalse();
        assertThat(result.getWeekDone()).isEqualTo(1);
        assertThat(result.getWeekScheduled()).isEqualTo(7);
    }

    @Test
    void complete_futureDay_tooOldDay_unscheduledDay_orArchived_isRejected() {
        Habit habit = habit((short) 0b0010000, 1); // Fridays only
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));

        assertError(ErrorConstant.HABIT_DATE_IN_FUTURE, () -> habitService.complete(MEMBER, "h-1", TODAY.plusDays(1), null));
        assertError(ErrorConstant.HABIT_DATE_TOO_OLD, () -> habitService.complete(MEMBER, "h-1", TODAY.minusDays(7), null));
        assertError(ErrorConstant.HABIT_NOT_SCHEDULED, () -> habitService.complete(MEMBER, "h-1", TODAY.minusDays(1), null));

        habit.setArchivedAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertError(ErrorConstant.HABIT_ARCHIVED, () -> habitService.complete(MEMBER, "h-1", TODAY, null));
        verify(completionRepository, never()).upsert(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void uncomplete_deletesTheDay() {
        Habit habit = habit(Habit.EVERY_DAY, 1);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));

        habitService.uncomplete(MEMBER, "h-1", TODAY.minusDays(6));

        verify(completionRepository).deleteDay("h-1", TODAY.minusDays(6));
    }

    // =========================================================================
    // lists
    // =========================================================================
    @Test
    void getToday_onlyActiveHabitsScheduledToday() {
        Habit daily = habit(Habit.EVERY_DAY, 1);
        Habit weekend = habit((short) 0b1100000, 1);
        weekend.setId("h-2");
        Habit archived = habit(Habit.EVERY_DAY, 1);
        archived.setId("h-3");
        archived.setArchivedAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        when(habitRepository.findAllOfMember(MEMBER)).thenReturn(List.of(daily, weekend, archived));

        assertThat(habitService.getToday(MEMBER).getData()).extracting(HabitResponse::getId).containsExactly("h-1");
        assertThat(habitService.getHabits(MEMBER, null, true).getData()).extracting(HabitResponse::getId).containsExactly("h-3");
        assertThat(habitService.getHabits(MEMBER, HabitCategory.PRODUCTIVITY, false).getData()).isEmpty();
    }

    @Test
    void archive_isIdempotent() {
        Habit habit = habit(Habit.EVERY_DAY, 1);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));

        assertThat(habitService.setArchived(MEMBER, "h-1", true).getData().isArchived()).isTrue();
        habitService.setArchived(MEMBER, "h-1", true);
        verify(habitRepository).save(habit);

        assertThat(habitService.setArchived(MEMBER, "h-1", false).getData().isArchived()).isFalse();
    }

    // =========================================================================
    // Phase 2: streak, calendar, reminder
    // =========================================================================
    @Test
    void habits_carryTheirStreak_computedFromTheDoneDays() {
        Habit habit = habit(Habit.EVERY_DAY, 1);
        when(habitRepository.findAllOfMember(MEMBER)).thenReturn(List.of(habit));
        when(completionRepository.findDoneDays(MEMBER, TODAY.minusDays(HabitService.STREAK_LOOKBACK_DAYS), TODAY))
                .thenReturn(List.of(new Object[]{"h-1", TODAY.minusDays(2)}, new Object[]{"h-1", TODAY.minusDays(1)}));

        HabitResponse result = habitService.getHabits(MEMBER, null, false).getData().get(0);

        assertThat(result.getCurrentStreak()).isEqualTo(2);
        assertThat(result.getBestStreak()).isEqualTo(2);
    }

    @Test
    void calendar_marksEachDayOfTheMonth() {
        // Mondays and Wednesdays only, target 2, started on 2026-10-05 (a Monday).
        Habit habit = habit(Habit.mask(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)), 2);
        habit.setStartDate(LocalDate.of(2026, 10, 5));
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));
        when(completionRepository.findByHabitIdAndLocalDateBetween("h-1", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(List.of(
                completion(LocalDate.of(2026, 10, 5), 2),    // Mon: done
                completion(LocalDate.of(2026, 10, 7), 1),    // Wed: partial
                completion(LocalDate.of(2026, 10, 10), 2))); // Sat: done although not scheduled

        HabitCalendarResponse result = habitService.getCalendar(MEMBER, "h-1", null).getData();

        assertThat(result.getMonth()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(result.getDays()).hasSize(31);
        assertThat(status(result, 1)).isEqualTo(HabitDayStatus.OUT_OF_RANGE);   // before the start
        assertThat(status(result, 5)).isEqualTo(HabitDayStatus.DONE);
        assertThat(status(result, 6)).isEqualTo(HabitDayStatus.NOT_SCHEDULED);
        assertThat(status(result, 7)).isEqualTo(HabitDayStatus.PARTIAL);
        assertThat(status(result, 10)).isEqualTo(HabitDayStatus.DONE);
        assertThat(status(result, 12)).isEqualTo(HabitDayStatus.MISSED);
        assertThat(status(result, 24)).isEqualTo(HabitDayStatus.OUT_OF_RANGE);  // future
        // Mon/Wed from the 5th to today (23rd): 5,7,12,14,19,21, plus the done Saturday = 7; done: 5 and 10.
        assertThat(result.getScheduledDays()).isEqualTo(7);
        assertThat(result.getDoneDays()).isEqualTo(2);
    }

    @Test
    void calendar_todayNotDoneIsPending() {
        Habit habit = habit(Habit.EVERY_DAY, 1);
        assertThat(HabitService.dayStatus(habit, TODAY, TODAY, 0)).isEqualTo(HabitDayStatus.PENDING);
        assertThat(HabitService.dayStatus(habit, TODAY, TODAY, 1)).isEqualTo(HabitDayStatus.DONE);
    }

    @Test
    void reminder_needsATime_andIsStoredPerMinute() {
        CreateHabitRequest noTime = createRequest(HabitScheduleType.DAILY, List.of());
        noTime.setReminderEnabled(true);
        BadRequestException missing = assertThrows(BadRequestException.class,
                () -> habitService.createHabit(MEMBER, noTime));
        assertThat(missing.getErrorConstant()).isEqualTo(ErrorConstant.HABIT_REMINDER_TIME_REQUIRED);

        CreateHabitRequest request = createRequest(HabitScheduleType.DAILY, List.of());
        request.setReminderEnabled(true);
        request.setReminderTime(LocalTime.of(7, 30, 45));
        HabitResponse created = habitService.createHabit(MEMBER, request).getData();
        assertThat(created.isReminderEnabled()).isTrue();
        assertThat(created.getReminderTime()).isEqualTo(LocalTime.of(7, 30));

        Habit habit = habit(Habit.EVERY_DAY, 1);
        habit.setReminderTime(LocalTime.of(7, 0));
        habit.setReminderEnabled(true);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));
        UpdateHabitRequest off = new UpdateHabitRequest();
        off.setReminderEnabled(false);
        assertThat(habitService.updateHabit(MEMBER, "h-1", off).getData().isReminderEnabled()).isFalse();
        assertThat(habit.getReminderTime()).isEqualTo(LocalTime.of(7, 0)); // kept for turning it on again
    }

    @Test
    void complete_countsAsTheFirstHabitCompletion() {
        Habit habit = habit(Habit.EVERY_DAY, 1);
        when(habitRepository.findByIdAndMemberIdAndDeletedAtIsNull("h-1", MEMBER)).thenReturn(Optional.of(habit));

        habitService.complete(MEMBER, "h-1", TODAY, null);

        verify(activationService).record(MEMBER, ActivationEvent.FIRST_HABIT_COMPLETION);
    }

    private static HabitDayStatus status(HabitCalendarResponse calendar, int dayOfMonth) {
        return calendar.getDays().get(dayOfMonth - 1).getStatus();
    }

    // ------------------------------------------------------------------
    private static void assertError(ErrorConstant expected, org.junit.jupiter.api.function.Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertThat(ex.getErrorConstant()).isEqualTo(expected);
    }

    private static CreateHabitRequest createRequest(HabitScheduleType type, List<DayOfWeek> days) {
        CreateHabitRequest request = new CreateHabitRequest();
        request.setName("Minum air");
        request.setCategory(HabitCategory.HEALTH);
        request.setIcon("local_drink");
        request.setColor("#22c55e");
        request.setScheduleType(type);
        request.setScheduleDays(days);
        return request;
    }

    private static Habit habit(short scheduleDays, int target) {
        Habit habit = new Habit();
        habit.setId("h-1");
        habit.setMemberId(MEMBER);
        habit.setName("Habit");
        habit.setCategory(HabitCategory.HEALTH);
        habit.setIcon("local_drink");
        habit.setColor("#22C55E");
        habit.setTargetCount(target);
        habit.setScheduleDays(scheduleDays);
        habit.setStartDate(LocalDate.of(2026, 9, 1));
        return habit;
    }

    private static HabitCompletion completion(LocalDate day, int count) {
        HabitCompletion completion = new HabitCompletion();
        completion.setHabitId("h-1");
        completion.setMemberId(MEMBER);
        completion.setLocalDate(day);
        completion.setCount(count);
        return completion;
    }
}
