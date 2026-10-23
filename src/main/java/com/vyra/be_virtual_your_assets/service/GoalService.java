package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateMilestoneRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalMilestoneResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressLogResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalRules;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateMilestoneRequest;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalMilestone;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.GoalMilestoneRepository;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Goals with milestones or a value towards a target. Progress is always computed here. The status is
 * only changed by the member: a passed due date or 100% progress never changes it.
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final GoalRepository goalRepository;
    private final GoalMilestoneRepository milestoneRepository;
    private final GoalProgressLogRepository progressLogRepository;
    private final ValidationService validationService;
    private final TimeService timeService;

    // =========================================================================
    // Read
    // =========================================================================

    /** The member's goals, all of them or only the given statuses; nearest due date first. */
    @Transactional(readOnly = true)
    public BaseResponse<List<GoalResponse>> getGoals(String memberId, List<GoalStatus> statuses) {
        List<Goal> goals = goalRepository.findAllOfMember(memberId).stream()
                .filter(g -> statuses == null || statuses.isEmpty() || statuses.contains(g.getStatus()))
                .toList();
        return new BaseResponse<>(ErrorConstant.GOAL_LIST_SUCCESS.getCode(),
                ErrorConstant.GOAL_LIST_SUCCESS.getMessage(), toResponses(goals, today(memberId)));
    }

    /** One goal with its milestones and its latest progress entries. */
    @Transactional(readOnly = true)
    public BaseResponse<GoalResponse> getGoal(String memberId, String goalId) {
        Goal goal = findOwned(memberId, goalId);
        return new BaseResponse<>(ErrorConstant.GOAL_DETAIL_SUCCESS.getCode(),
                ErrorConstant.GOAL_DETAIL_SUCCESS.getMessage(), toDetail(goal, today(memberId)));
    }

    // =========================================================================
    // Goal
    // =========================================================================

    @Transactional
    public BaseResponse<GoalResponse> createGoal(String memberId, CreateGoalRequest request) {
        if (goalRepository.countByMemberIdAndDeletedAtIsNull(memberId) >= GoalRules.MAX_GOALS) {
            throw new BadRequestException(ErrorConstant.GOAL_LIMIT_REACHED);
        }
        LocalDate today = today(memberId);
        if (request.getProgressType() == GoalProgressType.VALUE && request.getTargetValue() == null) {
            throw new BadRequestException(ErrorConstant.GOAL_TARGET_REQUIRED);
        }
        if (request.getProgressType() == GoalProgressType.MILESTONE && request.getTargetValue() != null) {
            throw new BadRequestException(ErrorConstant.GOAL_VALUE_NOT_ALLOWED);
        }
        checkDueDate(request.getDueDate(), today);

        LocalDateTime now = timeService.nowUtc();
        Goal goal = new Goal();
        goal.setMemberId(memberId);
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(StringUtils.trimToNull(request.getDescription()));
        goal.setCategory(request.getCategory());
        goal.setIcon(request.getIcon());
        goal.setColor(request.getColor().toUpperCase());
        goal.setProgressType(request.getProgressType());
        goal.setTargetValue(request.getTargetValue());
        goal.setUnit(StringUtils.trimToNull(request.getUnit()));
        goal.setDueDate(request.getDueDate());
        goal.setCreatedBy(memberId);
        goal.setCreatedAt(now);
        goalRepository.save(goal);

        List<String> titles = request.getMilestones() != null ? request.getMilestones() : List.of();
        List<GoalMilestone> milestones = new ArrayList<>(titles.size());
        for (int i = 0; i < titles.size(); i++) {
            milestones.add(milestone(goal.getId(), titles.get(i), i + 1, now));
        }
        milestoneRepository.saveAll(milestones);

        return new BaseResponse<>(ErrorConstant.GOAL_CREATE_SUCCESS.getCode(),
                ErrorConstant.GOAL_CREATE_SUCCESS.getMessage(), toResponse(goal, milestones, List.of(), today));
    }

    @Transactional
    public BaseResponse<GoalResponse> updateGoal(String memberId, String goalId, UpdateGoalRequest request) {
        Goal goal = findOwnedForUpdate(memberId, goalId);
        LocalDate today = today(memberId);
        if (request.getTitle() != null) {
            goal.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            goal.setDescription(StringUtils.trimToNull(request.getDescription()));
        }
        if (request.getCategory() != null) {
            goal.setCategory(request.getCategory());
        }
        if (request.getIcon() != null) {
            goal.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            goal.setColor(request.getColor().toUpperCase());
        }
        if (request.getTargetValue() != null) {
            if (goal.getProgressType() != GoalProgressType.VALUE) {
                throw new BadRequestException(ErrorConstant.GOAL_VALUE_NOT_ALLOWED);
            }
            goal.setTargetValue(request.getTargetValue());
        }
        if (request.getUnit() != null) {
            goal.setUnit(StringUtils.trimToNull(request.getUnit()));
        }
        if (request.isClearDueDate()) {
            goal.setDueDate(null);
        } else if (request.getDueDate() != null && !request.getDueDate().equals(goal.getDueDate())) {
            // An unchanged due date that has passed can be sent back as it is.
            checkDueDate(request.getDueDate(), today);
            goal.setDueDate(request.getDueDate());
        }
        if (request.getStatus() != null && request.getStatus() != goal.getStatus()) {
            goal.setStatus(request.getStatus());
            goal.setCompletedAt(request.getStatus() == GoalStatus.COMPLETED ? timeService.nowUtc() : null);
        }
        touch(goal, memberId);
        goalRepository.save(goal);
        return new BaseResponse<>(ErrorConstant.GOAL_UPDATE_SUCCESS.getCode(),
                ErrorConstant.GOAL_UPDATE_SUCCESS.getMessage(), toDetail(goal, today));
    }

    /** Soft delete; milestones and progress entries stay in the database. */
    @Transactional
    public BaseResponse<Void> deleteGoal(String memberId, String goalId) {
        Goal goal = findOwnedForUpdate(memberId, goalId);
        goal.setDeletedAt(timeService.nowUtc());
        goal.setModifiedBy(memberId);
        goalRepository.save(goal);
        return new BaseResponse<>(ErrorConstant.GOAL_DELETE_SUCCESS.getCode(),
                ErrorConstant.GOAL_DELETE_SUCCESS.getMessage(), null);
    }

    /**
     * Adds a value change (VALUE goals; negative to correct, never below 0) and/or a note. The goal row
     * is locked, so parallel requests add up.
     */
    @Transactional
    public BaseResponse<GoalResponse> addProgress(String memberId, String goalId, GoalProgressRequest request) {
        Goal goal = findOwnedForUpdate(memberId, goalId);
        BigDecimal delta = request.getDelta() != null && request.getDelta().signum() != 0 ? request.getDelta() : null;
        String note = StringUtils.trimToNull(request.getNote());
        if (delta == null && note == null) {
            throw new BadRequestException(ErrorConstant.GOAL_PROGRESS_EMPTY);
        }

        GoalProgressLog log = new GoalProgressLog();
        log.setGoalId(goal.getId());
        log.setNote(note);
        log.setCreatedAt(timeService.nowUtc());
        if (delta != null) {
            if (goal.getProgressType() != GoalProgressType.VALUE) {
                throw new BadRequestException(ErrorConstant.GOAL_VALUE_NOT_ALLOWED);
            }
            BigDecimal value = goal.getCurrentValue().add(delta);
            if (value.signum() < 0) {
                throw new BadRequestException(ErrorConstant.GOAL_VALUE_NEGATIVE);
            }
            goal.setCurrentValue(value);
            log.setDelta(delta);
            log.setValueAfter(value);
        }
        progressLogRepository.save(log);
        touch(goal, memberId);
        goalRepository.save(goal);
        return new BaseResponse<>(ErrorConstant.GOAL_PROGRESS_SUCCESS.getCode(),
                ErrorConstant.GOAL_PROGRESS_SUCCESS.getMessage(), toDetail(goal, today(memberId)));
    }

    // =========================================================================
    // Milestones
    // =========================================================================

    /** Appends a milestone at the end. */
    @Transactional
    public BaseResponse<GoalResponse> addMilestone(String memberId, String goalId, CreateMilestoneRequest request) {
        Goal goal = findOwnedForUpdate(memberId, goalId);
        if (milestoneRepository.countByGoalId(goal.getId()) >= GoalRules.MAX_MILESTONES) {
            throw new BadRequestException(ErrorConstant.GOAL_MILESTONE_LIMIT_REACHED);
        }
        milestoneRepository.save(milestone(goal.getId(), request.getTitle(),
                milestoneRepository.maxPosition(goal.getId()) + 1, timeService.nowUtc()));
        touch(goal, memberId);
        goalRepository.save(goal);
        return new BaseResponse<>(ErrorConstant.GOAL_MILESTONE_CREATE_SUCCESS.getCode(),
                ErrorConstant.GOAL_MILESTONE_CREATE_SUCCESS.getMessage(), toDetail(goal, today(memberId)));
    }

    /** Renames and/or ticks a milestone (idempotent: ticking a done one keeps its time). */
    @Transactional
    public BaseResponse<GoalResponse> updateMilestone(String memberId, String goalId, String milestoneId,
                                                      UpdateMilestoneRequest request) {
        Goal goal = findOwnedForUpdate(memberId, goalId);
        GoalMilestone milestone = findMilestone(goal, milestoneId);
        if (request.getTitle() != null) {
            milestone.setTitle(request.getTitle().trim());
        }
        if (request.getCompleted() != null && request.getCompleted() != milestone.isCompleted()) {
            milestone.setCompletedAt(request.getCompleted() ? timeService.nowUtc() : null);
        }
        milestoneRepository.save(milestone);
        touch(goal, memberId);
        goalRepository.save(goal);
        return new BaseResponse<>(ErrorConstant.GOAL_MILESTONE_UPDATE_SUCCESS.getCode(),
                ErrorConstant.GOAL_MILESTONE_UPDATE_SUCCESS.getMessage(), toDetail(goal, today(memberId)));
    }

    @Transactional
    public BaseResponse<GoalResponse> deleteMilestone(String memberId, String goalId, String milestoneId) {
        Goal goal = findOwnedForUpdate(memberId, goalId);
        milestoneRepository.delete(findMilestone(goal, milestoneId));
        touch(goal, memberId);
        goalRepository.save(goal);
        return new BaseResponse<>(ErrorConstant.GOAL_MILESTONE_DELETE_SUCCESS.getCode(),
                ErrorConstant.GOAL_MILESTONE_DELETE_SUCCESS.getMessage(), toDetail(goal, today(memberId)));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void checkDueDate(LocalDate dueDate, LocalDate today) {
        if (dueDate != null && dueDate.isBefore(today)) {
            throw new BadRequestException(ErrorConstant.GOAL_DUE_DATE_IN_PAST);
        }
    }

    private static GoalMilestone milestone(String goalId, String title, int position, LocalDateTime now) {
        GoalMilestone milestone = new GoalMilestone();
        milestone.setGoalId(goalId);
        milestone.setTitle(title.trim());
        milestone.setPosition(position);
        milestone.setCreatedAt(now);
        return milestone;
    }

    private Goal findOwned(String memberId, String goalId) {
        return goalRepository.findByIdAndMemberIdAndDeletedAtIsNull(goalId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.GOAL_NOT_FOUND));
    }

    private Goal findOwnedForUpdate(String memberId, String goalId) {
        return goalRepository.findOwnedForUpdate(goalId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.GOAL_NOT_FOUND));
    }

    private GoalMilestone findMilestone(Goal goal, String milestoneId) {
        return milestoneRepository.findByIdAndGoalId(milestoneId, goal.getId())
                .orElseThrow(() -> new NotFoundException(ErrorConstant.GOAL_MILESTONE_NOT_FOUND));
    }

    private void touch(Goal goal, String memberId) {
        goal.setModifiedBy(memberId);
        goal.setUpdatedAt(timeService.nowUtc());
    }

    private LocalDate today(String memberId) {
        return timeService.today(timeService.zoneOf(validationService.getMemberById(memberId)));
    }

    private GoalResponse toDetail(Goal goal, LocalDate today) {
        return toResponse(goal, milestoneRepository.findAllOfGoals(List.of(goal.getId())),
                progressLogRepository.findTop20ByGoalIdOrderByCreatedAtDesc(goal.getId()), today);
    }

    /** List responses without progress entries; one query for the milestones of all goals. */
    private List<GoalResponse> toResponses(List<Goal> goals, LocalDate today) {
        if (goals.isEmpty()) {
            return List.of();
        }
        Map<String, List<GoalMilestone>> milestones = milestoneRepository
                .findAllOfGoals(goals.stream().map(Goal::getId).toList()).stream()
                .collect(Collectors.groupingBy(GoalMilestone::getGoalId));
        return goals.stream()
                .map(g -> toResponse(g, milestones.getOrDefault(g.getId(), List.of()), null, today))
                .toList();
    }

    private static GoalResponse toResponse(Goal goal, List<GoalMilestone> milestones, List<GoalProgressLog> logs,
                                           LocalDate today) {
        int done = (int) milestones.stream().filter(GoalMilestone::isCompleted).count();
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setTitle(goal.getTitle());
        response.setDescription(goal.getDescription());
        response.setCategory(goal.getCategory());
        response.setIcon(goal.getIcon());
        response.setColor(goal.getColor());
        response.setProgressType(goal.getProgressType());
        response.setTargetValue(goal.getTargetValue());
        response.setCurrentValue(goal.getCurrentValue());
        response.setUnit(goal.getUnit());
        response.setDueDate(goal.getDueDate());
        response.setStatus(goal.getStatus());
        response.setCompletedAt(goal.getCompletedAt());
        response.setCreatedAt(goal.getCreatedAt());
        response.setMilestoneDone(done);
        response.setMilestoneTotal(milestones.size());
        response.setProgressPercent(progressPercent(goal, done, milestones.size()));
        response.setOverdue(goal.getStatus() == GoalStatus.ACTIVE && goal.getDueDate() != null
                && goal.getDueDate().isBefore(today));
        response.setMilestones(milestones.stream()
                .map(m -> new GoalMilestoneResponse(m.getId(), m.getTitle(), m.getPosition(), m.isCompleted(),
                        m.getCompletedAt()))
                .toList());
        if (logs != null) {
            response.setProgressLogs(logs.stream()
                    .map(l -> new GoalProgressLogResponse(l.getId(), l.getDelta(), l.getValueAfter(), l.getNote(),
                            l.getCreatedAt()))
                    .toList());
        }
        return response;
    }

    /** 0..100, rounded down so 100 only shows when the goal is really reached. */
    static int progressPercent(Goal goal, int milestonesDone, int milestonesTotal) {
        if (goal.getProgressType() == GoalProgressType.MILESTONE) {
            return milestonesTotal == 0 ? 0 : milestonesDone * 100 / milestonesTotal;
        }
        if (goal.getTargetValue() == null || goal.getTargetValue().signum() <= 0) {
            return 0;
        }
        return goal.getCurrentValue().multiply(HUNDRED)
                .divide(goal.getTargetValue(), 0, RoundingMode.DOWN)
                .min(HUNDRED)
                .intValue();
    }
}
