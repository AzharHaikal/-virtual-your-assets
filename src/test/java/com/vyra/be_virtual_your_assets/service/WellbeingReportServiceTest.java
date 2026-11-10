package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.GoalReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.HabitReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.MoodReportItemResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.GoalMilestoneRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WellbeingReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 7);

    @Mock private MoodEntryRepository moodEntryRepository;
    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository habitCompletionRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private GoalMilestoneRepository goalMilestoneRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;

    private WellbeingReportService service;

    @BeforeEach
    void setUp() {
        // "Today" is 2026-10-20 in Jakarta, after the report period.
        TimeService timeService = new TimeService(Clock.fixed(Instant.parse("2026-10-20T05:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        service = new WellbeingReportService(moodEntryRepository, habitRepository, habitCompletionRepository,
                goalRepository, goalMilestoneRepository, adminAuthorizationService, adminActivityService, timeService);
        lenient().when(adminAuthorizationService.getCurrentAdmin())
                .thenReturn(new CustomAdminDetails("super-admin-id", "token", AdminRole.SUPER_ADMIN));
    }

    // ─── access ──────────────────────────────────────────────────────────────

    @Test
    void everyReport_withoutWellbeingPermission_isForbiddenAndReadsNothing() {
        doThrow(new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED))
                .when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_WELLBEING_VIEW);

        assertThrows(ForbiddenException.class, () -> service.getMoodReport(FROM, TO, null, 0, 10));
        assertThrows(ForbiddenException.class, () -> service.getHabitReport(FROM, TO, null, 0, 10));
        assertThrows(ForbiddenException.class, () -> service.getGoalReport(null, null, 0, 10));

        verifyNoInteractions(moodEntryRepository, habitRepository, habitCompletionRepository,
                goalRepository, goalMilestoneRepository, adminActivityService);
    }

    @Test
    void firstPage_isWrittenToTheAuditTrail_laterPagesAreNot() {
        when(goalRepository.findForReport(any(), anyString(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getGoalReport(null, null, 0, 10);
        service.getGoalReport(null, null, 1, 10);

        verify(adminActivityService).createAdminActivity(eq("super-admin-id"), eq(null),
                eq(AdminActivityEvent.VIEW_MEMBER_WELLBEING), eq("goal report"), eq(null), eq(null));
    }

    // ─── mood ────────────────────────────────────────────────────────────────

    @Test
    void getMoodReport_mapsEntriesWithMemberTagsAndNote() {
        MoodEntry entry = new MoodEntry();
        entry.setId("entry-1");
        entry.setLocalDate(LocalDate.of(2026, 10, 3));
        entry.setMoodScore((short) 4);
        entry.setNote("private journal");
        entry.setCheckedInAt(LocalDateTime.of(2026, 10, 3, 2, 0));
        when(moodEntryRepository.findForReport(eq(FROM), eq(TO), eq("%budi%"), any()))
                .thenReturn(new PageImpl<>(List.<Object[]>of(new Object[]{entry, member()}), PageRequest.of(0, 10), 1));
        when(moodEntryRepository.findTagNames(List.of("entry-1")))
                .thenReturn(List.of(new Object[]{"entry-1", "Work"}, new Object[]{"entry-1", "Family"}));

        BaseResponse<PageResponse<MoodReportItemResponse>> result = service.getMoodReport(FROM, TO, "  Budi ", 0, 10);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REPORT_MOOD_SUCCESS.getCode());
        MoodReportItemResponse item = result.getData().getContent().get(0);
        assertThat(item.getMemberName()).isEqualTo("Budi Santoso");
        assertThat(item.getMemberEmail()).isEqualTo("budi@example.com");
        assertThat(item.getMoodScore()).isEqualTo(4);
        assertThat(item.getTags()).containsExactly("Work", "Family");
        assertThat(item.getNote()).isEqualTo("private journal");
    }

    @Test
    void getMoodReport_invertedRange_returnsEmptyWithoutQuerying() {
        BaseResponse<PageResponse<MoodReportItemResponse>> result = service.getMoodReport(TO, FROM, null, 0, 10);

        assertThat(result.getData().getContent()).isEmpty();
        verify(moodEntryRepository, never()).findForReport(any(), any(), anyString(), any());
    }

    // ─── habits ──────────────────────────────────────────────────────────────

    @Test
    void getHabitReport_countsScheduledAndCompletedDaysInThePeriod() {
        Habit habit = habit();
        when(habitRepository.findForReport(eq(TO), eq("%"), any()))
                .thenReturn(new PageImpl<>(List.<Object[]>of(new Object[]{habit, member()}), PageRequest.of(0, 10), 1));
        when(habitCompletionRepository.summarizeForReport(List.of("habit-1"), FROM, TO))
                .thenReturn(List.<Object[]>of(new Object[]{"habit-1", 2L, LocalDate.of(2026, 10, 5)}));

        HabitReportItemResponse item = service.getHabitReport(FROM, TO, null, 0, 10).getData().getContent().get(0);

        // 2026-10-01..07 is Thu..Wed: Monday, Wednesday and Friday fall on 10-05, 10-07 and 10-02.
        assertThat(item.getScheduledDays()).isEqualTo(3);
        assertThat(item.getCompletedDays()).isEqualTo(2);
        assertThat(item.getCompletionRate()).isEqualTo(66);
        assertThat(item.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(item.getScheduleDays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void scheduledDays_stopsBeforeTheArchiveDayAndAtTheMembersToday() {
        Habit habit = habit();
        habit.setScheduleDays(Habit.EVERY_DAY);
        // Archived on 2026-10-04 (Jakarta): 10-01..10-03 count.
        habit.setArchivedAt(LocalDateTime.of(2026, 10, 4, 3, 0));
        assertThat(service.scheduledDays(habit, member(), FROM, TO)).isEqualTo(3);

        habit.setArchivedAt(null);
        // The period runs into the future: days after today (2026-10-20) do not count.
        assertThat(service.scheduledDays(habit, member(), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 31))).isEqualTo(3);
    }

    // ─── goals ───────────────────────────────────────────────────────────────

    @Test
    void getGoalReport_usesTheAppProgressRuleAndFiltersByStatus() {
        Goal goal = new Goal();
        goal.setId("goal-1");
        goal.setTitle("Emergency fund");
        goal.setProgressType(GoalProgressType.VALUE);
        goal.setCurrentValue(new BigDecimal("2500000"));
        goal.setTargetValue(new BigDecimal("10000000"));
        goal.setStatus(GoalStatus.ACTIVE);
        when(goalRepository.findForReport(eq(List.of(GoalStatus.ACTIVE)), eq("%"), any()))
                .thenReturn(new PageImpl<>(List.<Object[]>of(new Object[]{goal, member()}), PageRequest.of(0, 10), 1));
        when(goalMilestoneRepository.countByGoals(List.of("goal-1")))
                .thenReturn(List.<Object[]>of(new Object[]{"goal-1", 4L, 1L}));

        GoalReportItemResponse item = service.getGoalReport(GoalStatus.ACTIVE, "", 0, 10).getData().getContent().get(0);

        assertThat(item.getProgressPercent()).isEqualTo(25);
        assertThat(item.getMilestonesTotal()).isEqualTo(4);
        assertThat(item.getMilestonesDone()).isEqualTo(1);
        assertThat(item.getMemberPhoneNumber()).isEqualTo("081234567890");
    }

    @Test
    void getGoalReport_withoutStatus_queriesEveryStatus() {
        when(goalRepository.findForReport(any(), anyString(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getGoalReport(null, null, 0, 10);

        verify(goalRepository).findForReport(eq(List.of(GoalStatus.values())), eq("%"), any());
        verify(goalMilestoneRepository, never()).countByGoals(any());
    }

    // ─── fixtures ────────────────────────────────────────────────────────────

    private static Member member() {
        Member member = new Member();
        member.setId("member-1");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("081234567890");
        member.setTimezone("Asia/Jakarta");
        return member;
    }

    private static Habit habit() {
        Habit habit = new Habit();
        habit.setId("habit-1");
        habit.setName("Read 10 pages");
        habit.setCategory(HabitCategory.PERSONAL);
        habit.setIcon("book");
        habit.setColor("#1685ff");
        habit.setTargetCount(1);
        habit.setScheduleDays(Habit.mask(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)));
        habit.setStartDate(LocalDate.of(2026, 9, 1));
        return habit;
    }
}
