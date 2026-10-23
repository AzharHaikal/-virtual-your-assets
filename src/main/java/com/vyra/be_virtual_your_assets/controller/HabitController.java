package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCompletionRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.habit.UpdateHabitRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.HabitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Habit tracker. Only for the member themself (no admin access). */
@RestController
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @GetMapping(ApiPath.V1_HABITS)
    public BaseResponse<List<HabitResponse>> getHabits(
            @RequestParam(required = false) HabitCategory category,
            @RequestParam(defaultValue = "false") boolean archived,
            Authentication authentication
    ) {
        return habitService.getHabits(memberId(authentication), category, archived);
    }

    @GetMapping(ApiPath.V1_HABITS + ApiPath.HABIT_TODAY)
    public BaseResponse<List<HabitResponse>> getToday(Authentication authentication) {
        return habitService.getToday(memberId(authentication));
    }

    @PostMapping(ApiPath.V1_HABITS)
    public ResponseEntity<BaseResponse<HabitResponse>> createHabit(
            @Valid @RequestBody CreateHabitRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(habitService.createHabit(memberId(authentication), request));
    }

    @PatchMapping(ApiPath.V1_HABITS + ApiPath.HABIT_ID)
    public BaseResponse<HabitResponse> updateHabit(
            @PathVariable String habitId,
            @Valid @RequestBody UpdateHabitRequest request,
            Authentication authentication
    ) {
        return habitService.updateHabit(memberId(authentication), habitId, request);
    }

    @DeleteMapping(ApiPath.V1_HABITS + ApiPath.HABIT_ID)
    public BaseResponse<Void> deleteHabit(@PathVariable String habitId, Authentication authentication) {
        return habitService.deleteHabit(memberId(authentication), habitId);
    }

    @PostMapping(ApiPath.V1_HABITS + ApiPath.HABIT_ARCHIVE)
    public BaseResponse<HabitResponse> archive(@PathVariable String habitId, Authentication authentication) {
        return habitService.setArchived(memberId(authentication), habitId, true);
    }

    @PostMapping(ApiPath.V1_HABITS + ApiPath.HABIT_UNARCHIVE)
    public BaseResponse<HabitResponse> unarchive(@PathVariable String habitId, Authentication authentication) {
        return habitService.setArchived(memberId(authentication), habitId, false);
    }

    /** Mark a day (yyyy-MM-dd, the member's local date): today or up to 6 days back. */
    @PutMapping(ApiPath.V1_HABITS + ApiPath.HABIT_COMPLETION)
    public BaseResponse<HabitResponse> complete(
            @PathVariable String habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate,
            @Valid @RequestBody(required = false) HabitCompletionRequest request,
            Authentication authentication
    ) {
        return habitService.complete(memberId(authentication), habitId, localDate, request);
    }

    @DeleteMapping(ApiPath.V1_HABITS + ApiPath.HABIT_COMPLETION)
    public BaseResponse<HabitResponse> uncomplete(
            @PathVariable String habitId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate,
            Authentication authentication
    ) {
        return habitService.uncomplete(memberId(authentication), habitId, localDate);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
