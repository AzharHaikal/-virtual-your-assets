package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateMilestoneRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalRules;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateMilestoneRequest;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalMilestone;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.GoalMilestoneRepository;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-22 17:30 UTC = 2026-10-23 00:30 in Jakarta. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 17, 30);

    @Mock private GoalRepository goalRepository;
    @Mock private GoalMilestoneRepository milestoneRepository;
    @Mock private GoalProgressLogRepository progressLogRepository;
    @Mock private ValidationService validationService;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        goalService = new GoalService(goalRepository, milestoneRepository, progressLogRepository,
                validationService, timeService);
        Member member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(milestoneRepository.findAllOfGoals(anyList())).thenReturn(List.of());
        lenient().when(progressLogRepository.findTop20ByGoalIdOrderByCreatedAtDesc(anyString())).thenReturn(List.of());
    }

    // =========================================================================
    // create
    // =========================================================================
    @Test
    void createGoal_valueGoal_storesTrimmedFieldsAndStartsAtZero() {
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
            Goal goal = inv.getArgument(0);
            goal.setId("g-new");
            return goal;
        });
        CreateGoalRequest request = createRequest(GoalProgressType.VALUE);
        request.setTargetValue(new BigDecimal("10000000"));
        request.setUnit("  IDR ");
        request.setDescription("   ");
        request.setDueDate(TODAY);

        GoalResponse result = goalService.createGoal(MEMBER, request).getData();

        ArgumentCaptor<Goal> saved = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Dana darurat");
        assertThat(saved.getValue().getColor()).isEqualTo("#22C55E");
        assertThat(saved.getValue().getUnit()).isEqualTo("IDR");
        assertThat(saved.getValue().getDescription()).isNull();
        assertThat(saved.getValue().getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(result.getCurrentValue()).isEqualByComparingTo("0");
        assertThat(result.getProgressPercent()).isZero();
        assertThat(result.isOverdue()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void createGoal_withMilestones_savesThemInOrder() {
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
            Goal goal = inv.getArgument(0);
            goal.setId("g-new");
            return goal;
        });
        CreateGoalRequest request = createRequest(GoalProgressType.MILESTONE);
        request.setMilestones(List.of(" Daftar kelas ", "Selesaikan modul 1"));

        GoalResponse result = goalService.createGoal(MEMBER, request).getData();

        ArgumentCaptor<List<GoalMilestone>> saved = ArgumentCaptor.forClass(List.class);
        verify(milestoneRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(GoalMilestone::getTitle).containsExactly("Daftar kelas", "Selesaikan modul 1");
        assertThat(saved.getValue()).extracting(GoalMilestone::getPosition).containsExactly(1, 2);
        assertThat(saved.getValue()).extracting(GoalMilestone::getGoalId).containsOnly("g-new");
        assertThat(result.getMilestoneTotal()).isEqualTo(2);
        assertThat(result.getMilestoneDone()).isZero();
    }

    @Test
    void createGoal_invalidCombinations_areRejected() {
        assertError(ErrorConstant.GOAL_TARGET_REQUIRED,
                () -> goalService.createGoal(MEMBER, createRequest(GoalProgressType.VALUE)));

        CreateGoalRequest milestoneWithTarget = createRequest(GoalProgressType.MILESTONE);
        milestoneWithTarget.setTargetValue(BigDecimal.TEN);
        assertError(ErrorConstant.GOAL_VALUE_NOT_ALLOWED, () -> goalService.createGoal(MEMBER, milestoneWithTarget));

        CreateGoalRequest pastDue = createRequest(GoalProgressType.MILESTONE);
        pastDue.setDueDate(TODAY.minusDays(1));
        assertError(ErrorConstant.GOAL_DUE_DATE_IN_PAST, () -> goalService.createGoal(MEMBER, pastDue));

        when(goalRepository.countByMemberIdAndDeletedAtIsNull(MEMBER)).thenReturn((long) GoalRules.MAX_GOALS);
        assertError(ErrorConstant.GOAL_LIMIT_REACHED,
                () -> goalService.createGoal(MEMBER, createRequest(GoalProgressType.MILESTONE)));
        verify(goalRepository, never()).save(any());
    }

    // =========================================================================
    // progress
    // =========================================================================
    @Test
    void progressPercent_isComputedOnTheServer_roundedDownAndCappedAt100() {
        Goal value = goal(GoalProgressType.VALUE);
        value.setTargetValue(new BigDecimal("10000000"));
        value.setCurrentValue(new BigDecimal("3000000"));
        assertThat(GoalService.progressPercent(value, 0, 0)).isEqualTo(30);
        value.setCurrentValue(new BigDecimal("9999999"));
        assertThat(GoalService.progressPercent(value, 0, 0)).isEqualTo(99);
        value.setCurrentValue(new BigDecimal("12000000"));
        assertThat(GoalService.progressPercent(value, 0, 0)).isEqualTo(100);

        Goal milestone = goal(GoalProgressType.MILESTONE);
        assertThat(GoalService.progressPercent(milestone, 2, 3)).isEqualTo(66);
        assertThat(GoalService.progressPercent(milestone, 0, 0)).isZero();
    }

    @Test
    void addProgress_addsTheDeltaAndLogsTheValueAfter() {
        Goal goal = goal(GoalProgressType.VALUE);
        goal.setTargetValue(new BigDecimal("10000000"));
        goal.setCurrentValue(new BigDecimal("3000000"));
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));
        GoalProgressRequest request = new GoalProgressRequest();
        request.setDelta(new BigDecimal("500000"));
        request.setNote("  Gaji Oktober ");

        GoalResponse result = goalService.addProgress(MEMBER, "g-1", request).getData();

        ArgumentCaptor<GoalProgressLog> log = ArgumentCaptor.forClass(GoalProgressLog.class);
        verify(progressLogRepository).save(log.capture());
        assertThat(log.getValue().getDelta()).isEqualByComparingTo("500000");
        assertThat(log.getValue().getValueAfter()).isEqualByComparingTo("3500000");
        assertThat(log.getValue().getNote()).isEqualTo("Gaji Oktober");
        assertThat(result.getCurrentValue()).isEqualByComparingTo("3500000");
        assertThat(result.getProgressPercent()).isEqualTo(35);
        // 100% never completes the goal by itself.
        assertThat(result.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void addProgress_noteOnlyOnAMilestoneGoal_isAccepted() {
        Goal goal = goal(GoalProgressType.MILESTONE);
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));
        GoalProgressRequest request = new GoalProgressRequest();
        request.setNote("Mulai belajar");
        request.setDelta(BigDecimal.ZERO);

        goalService.addProgress(MEMBER, "g-1", request);

        ArgumentCaptor<GoalProgressLog> log = ArgumentCaptor.forClass(GoalProgressLog.class);
        verify(progressLogRepository).save(log.capture());
        assertThat(log.getValue().getDelta()).isNull();
        assertThat(log.getValue().getValueAfter()).isNull();
    }

    @Test
    void addProgress_belowZero_valueOnMilestoneGoal_orEmpty_isRejected() {
        Goal value = goal(GoalProgressType.VALUE);
        value.setTargetValue(BigDecimal.TEN);
        value.setCurrentValue(new BigDecimal("2"));
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(value));
        GoalProgressRequest tooMuch = new GoalProgressRequest();
        tooMuch.setDelta(new BigDecimal("-3"));
        assertError(ErrorConstant.GOAL_VALUE_NEGATIVE, () -> goalService.addProgress(MEMBER, "g-1", tooMuch));
        assertError(ErrorConstant.GOAL_PROGRESS_EMPTY,
                () -> goalService.addProgress(MEMBER, "g-1", new GoalProgressRequest()));

        Goal milestone = goal(GoalProgressType.MILESTONE);
        milestone.setId("g-2");
        when(goalRepository.findOwnedForUpdate("g-2", MEMBER)).thenReturn(Optional.of(milestone));
        GoalProgressRequest delta = new GoalProgressRequest();
        delta.setDelta(BigDecimal.ONE);
        assertError(ErrorConstant.GOAL_VALUE_NOT_ALLOWED, () -> goalService.addProgress(MEMBER, "g-2", delta));

        verify(progressLogRepository, never()).save(any());
        assertThat(value.getCurrentValue()).isEqualByComparingTo("2");
    }

    // =========================================================================
    // update / delete
    // =========================================================================
    @Test
    void updateGoal_statusCompletedSetsCompletedAt_andActiveClearsIt() {
        Goal goal = goal(GoalProgressType.MILESTONE);
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));
        UpdateGoalRequest complete = new UpdateGoalRequest();
        complete.setStatus(GoalStatus.COMPLETED);

        assertThat(goalService.updateGoal(MEMBER, "g-1", complete).getData().getCompletedAt()).isEqualTo(NOW);

        UpdateGoalRequest reopen = new UpdateGoalRequest();
        reopen.setStatus(GoalStatus.ACTIVE);
        assertThat(goalService.updateGoal(MEMBER, "g-1", reopen).getData().getCompletedAt()).isNull();
    }

    @Test
    void updateGoal_passedDueDate_canBeKeptButNotSet_andIsOverdueWhileActive() {
        Goal goal = goal(GoalProgressType.MILESTONE);
        goal.setDueDate(TODAY.minusDays(3));
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));

        UpdateGoalRequest same = new UpdateGoalRequest();
        same.setTitle("  Belajar Inggris ");
        same.setDueDate(TODAY.minusDays(3));
        GoalResponse result = goalService.updateGoal(MEMBER, "g-1", same).getData();
        assertThat(result.getTitle()).isEqualTo("Belajar Inggris");
        assertThat(result.isOverdue()).isTrue();
        // A passed due date never changes the status.
        assertThat(result.getStatus()).isEqualTo(GoalStatus.ACTIVE);

        UpdateGoalRequest earlier = new UpdateGoalRequest();
        earlier.setDueDate(TODAY.minusDays(4));
        assertError(ErrorConstant.GOAL_DUE_DATE_IN_PAST, () -> goalService.updateGoal(MEMBER, "g-1", earlier));

        UpdateGoalRequest clear = new UpdateGoalRequest();
        clear.setClearDueDate(true);
        assertThat(goalService.updateGoal(MEMBER, "g-1", clear).getData().getDueDate()).isNull();
    }

    @Test
    void updateGoal_targetOnAMilestoneGoal_isRejected() {
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal(GoalProgressType.MILESTONE)));
        UpdateGoalRequest request = new UpdateGoalRequest();
        request.setTargetValue(BigDecimal.TEN);

        assertError(ErrorConstant.GOAL_VALUE_NOT_ALLOWED, () -> goalService.updateGoal(MEMBER, "g-1", request));
    }

    @Test
    void anotherMembersGoal_is404() {
        when(goalRepository.findOwnedForUpdate("g-x", MEMBER)).thenReturn(Optional.empty());
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-x", MEMBER)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> goalService.getGoal(MEMBER, "g-x"));
        assertThrows(NotFoundException.class, () -> goalService.deleteGoal(MEMBER, "g-x"));
        assertThrows(NotFoundException.class, () -> goalService.addProgress(MEMBER, "g-x", new GoalProgressRequest()));
        assertThrows(NotFoundException.class, () -> goalService.addMilestone(MEMBER, "g-x", new CreateMilestoneRequest()));
    }

    @Test
    void deleteGoal_isSoft() {
        Goal goal = goal(GoalProgressType.MILESTONE);
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(MEMBER, "g-1");

        assertThat(goal.getDeletedAt()).isEqualTo(NOW);
        verify(goalRepository).save(goal);
    }

    // =========================================================================
    // milestones
    // =========================================================================
    @Test
    void addMilestone_appendsAtTheEnd_withinTheLimit() {
        Goal goal = goal(GoalProgressType.MILESTONE);
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));
        when(milestoneRepository.maxPosition("g-1")).thenReturn(3);
        CreateMilestoneRequest request = new CreateMilestoneRequest();
        request.setTitle(" Ujian akhir ");

        goalService.addMilestone(MEMBER, "g-1", request);

        ArgumentCaptor<GoalMilestone> saved = ArgumentCaptor.forClass(GoalMilestone.class);
        verify(milestoneRepository).save(saved.capture());
        assertThat(saved.getValue().getPosition()).isEqualTo(4);
        assertThat(saved.getValue().getTitle()).isEqualTo("Ujian akhir");

        when(milestoneRepository.countByGoalId("g-1")).thenReturn((long) GoalRules.MAX_MILESTONES);
        assertError(ErrorConstant.GOAL_MILESTONE_LIMIT_REACHED, () -> goalService.addMilestone(MEMBER, "g-1", request));
    }

    @Test
    void updateMilestone_tickIsIdempotent_andTheProgressFollows() {
        Goal goal = goal(GoalProgressType.MILESTONE);
        GoalMilestone first = milestone("m-1", 1);
        GoalMilestone second = milestone("m-2", 2);
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal));
        when(milestoneRepository.findByIdAndGoalId("m-1", "g-1")).thenReturn(Optional.of(first));
        when(milestoneRepository.findAllOfGoals(List.of("g-1"))).thenReturn(List.of(first, second));
        UpdateMilestoneRequest tick = new UpdateMilestoneRequest();
        tick.setCompleted(true);

        GoalResponse result = goalService.updateMilestone(MEMBER, "g-1", "m-1", tick).getData();
        assertThat(first.getCompletedAt()).isEqualTo(NOW);
        assertThat(result.getMilestoneDone()).isEqualTo(1);
        assertThat(result.getProgressPercent()).isEqualTo(50);

        first.setCompletedAt(NOW.minusDays(1));
        goalService.updateMilestone(MEMBER, "g-1", "m-1", tick);
        assertThat(first.getCompletedAt()).isEqualTo(NOW.minusDays(1));
    }

    @Test
    void milestoneOfAnotherGoal_is404() {
        when(goalRepository.findOwnedForUpdate("g-1", MEMBER)).thenReturn(Optional.of(goal(GoalProgressType.MILESTONE)));
        when(milestoneRepository.findByIdAndGoalId("m-x", "g-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> goalService.updateMilestone(MEMBER, "g-1", "m-x", new UpdateMilestoneRequest()));
        assertThrows(NotFoundException.class, () -> goalService.deleteMilestone(MEMBER, "g-1", "m-x"));
    }

    // =========================================================================
    // list
    // =========================================================================
    @Test
    void getGoals_filtersByStatus_andCarriesMilestoneCounts() {
        Goal active = goal(GoalProgressType.MILESTONE);
        Goal paused = goal(GoalProgressType.MILESTONE);
        paused.setId("g-2");
        paused.setStatus(GoalStatus.PAUSED);
        Goal done = goal(GoalProgressType.MILESTONE);
        done.setId("g-3");
        done.setStatus(GoalStatus.COMPLETED);
        when(goalRepository.findAllOfMember(MEMBER)).thenReturn(List.of(active, paused, done));
        GoalMilestone milestone = milestone("m-1", 1);
        milestone.setCompletedAt(NOW);
        when(milestoneRepository.findAllOfGoals(List.of("g-1", "g-2"))).thenReturn(List.of(milestone));

        List<GoalResponse> result = goalService.getGoals(MEMBER, List.of(GoalStatus.ACTIVE, GoalStatus.PAUSED)).getData();

        assertThat(result).extracting(GoalResponse::getId).containsExactly("g-1", "g-2");
        assertThat(result.get(0).getProgressPercent()).isEqualTo(100);
        assertThat(result.get(0).getProgressLogs()).isNull();
        assertThat(result.get(1).getMilestoneTotal()).isZero();
        assertThat(goalService.getGoals(MEMBER, null).getData()).hasSize(3);
    }

    // ------------------------------------------------------------------
    private static void assertError(ErrorConstant expected, org.junit.jupiter.api.function.Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertThat(ex.getErrorConstant()).isEqualTo(expected);
    }

    private static CreateGoalRequest createRequest(GoalProgressType type) {
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle("  Dana darurat ");
        request.setCategory(GoalCategory.FINANCE);
        request.setIcon("savings");
        request.setColor("#22c55e");
        request.setProgressType(type);
        return request;
    }

    private static Goal goal(GoalProgressType type) {
        Goal goal = new Goal();
        goal.setId("g-1");
        goal.setMemberId(MEMBER);
        goal.setTitle("Goal");
        goal.setCategory(GoalCategory.EDUCATION);
        goal.setIcon("school");
        goal.setColor("#3B82F6");
        goal.setProgressType(type);
        return goal;
    }

    private static GoalMilestone milestone(String id, int position) {
        GoalMilestone milestone = new GoalMilestone();
        milestone.setId(id);
        milestone.setGoalId("g-1");
        milestone.setTitle("Step " + position);
        milestone.setPosition(position);
        milestone.setCreatedAt(NOW);
        return milestone;
    }
}
