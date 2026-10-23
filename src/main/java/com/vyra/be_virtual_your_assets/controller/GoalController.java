package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateMilestoneRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateMilestoneRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Goals & milestones. Only for the member themself (no admin access). */
@RestController
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /** {@code ?status=ACTIVE&status=PAUSED}; without status all goals. */
    @GetMapping(ApiPath.V1_GOALS)
    public BaseResponse<List<GoalResponse>> getGoals(
            @RequestParam(required = false) List<GoalStatus> status,
            Authentication authentication
    ) {
        return goalService.getGoals(memberId(authentication), status);
    }

    @GetMapping(ApiPath.V1_GOALS + ApiPath.GOAL_ID)
    public BaseResponse<GoalResponse> getGoal(@PathVariable String goalId, Authentication authentication) {
        return goalService.getGoal(memberId(authentication), goalId);
    }

    @PostMapping(ApiPath.V1_GOALS)
    public ResponseEntity<BaseResponse<GoalResponse>> createGoal(
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(memberId(authentication), request));
    }

    @PatchMapping(ApiPath.V1_GOALS + ApiPath.GOAL_ID)
    public BaseResponse<GoalResponse> updateGoal(
            @PathVariable String goalId,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication
    ) {
        return goalService.updateGoal(memberId(authentication), goalId, request);
    }

    @DeleteMapping(ApiPath.V1_GOALS + ApiPath.GOAL_ID)
    public BaseResponse<Void> deleteGoal(@PathVariable String goalId, Authentication authentication) {
        return goalService.deleteGoal(memberId(authentication), goalId);
    }

    @PostMapping(ApiPath.V1_GOALS + ApiPath.GOAL_PROGRESS)
    public BaseResponse<GoalResponse> addProgress(
            @PathVariable String goalId,
            @Valid @RequestBody GoalProgressRequest request,
            Authentication authentication
    ) {
        return goalService.addProgress(memberId(authentication), goalId, request);
    }

    @PostMapping(ApiPath.V1_GOALS + ApiPath.GOAL_MILESTONES)
    public ResponseEntity<BaseResponse<GoalResponse>> addMilestone(
            @PathVariable String goalId,
            @Valid @RequestBody CreateMilestoneRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.addMilestone(memberId(authentication), goalId, request));
    }

    @PatchMapping(ApiPath.V1_GOALS + ApiPath.GOAL_MILESTONE_ID)
    public BaseResponse<GoalResponse> updateMilestone(
            @PathVariable String goalId,
            @PathVariable String milestoneId,
            @Valid @RequestBody UpdateMilestoneRequest request,
            Authentication authentication
    ) {
        return goalService.updateMilestone(memberId(authentication), goalId, milestoneId, request);
    }

    @DeleteMapping(ApiPath.V1_GOALS + ApiPath.GOAL_MILESTONE_ID)
    public BaseResponse<GoalResponse> deleteMilestone(
            @PathVariable String goalId,
            @PathVariable String milestoneId,
            Authentication authentication
    ) {
        return goalService.deleteMilestone(memberId(authentication), goalId, milestoneId);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
