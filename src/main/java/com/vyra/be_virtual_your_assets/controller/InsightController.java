package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.insight.AdvancedInsightResponse;
import com.vyra.be_virtual_your_assets.dto.insight.AiReflectionResponse;
import com.vyra.be_virtual_your_assets.dto.insight.AiReflectionStatusResponse;
import com.vyra.be_virtual_your_assets.dto.insight.WeeklyInsightResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.AdvancedInsightService;
import com.vyra.be_virtual_your_assets.service.AiReflectionService;
import com.vyra.be_virtual_your_assets.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Summaries across modules and AI reflections. Only for the member themself. */
@RestController
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final AdvancedInsightService advancedInsightService;
    private final AiReflectionService aiReflectionService;

    /** date = any day of the week (yyyy-MM-dd); default today in the member's zone. */
    @GetMapping(ApiPath.V1_INSIGHTS_WEEKLY)
    public BaseResponse<WeeklyInsightResponse> getWeekly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return insightService.getWeekly(memberId(authentication), date);
    }

    @GetMapping(ApiPath.V1_INSIGHTS_ADVANCED)
    public BaseResponse<AdvancedInsightResponse> getAdvanced(Authentication authentication) {
        return advancedInsightService.get(memberId(authentication));
    }

    @GetMapping(ApiPath.V1_REFLECTIONS + ApiPath.REFLECTIONS_STATUS)
    public BaseResponse<AiReflectionStatusResponse> getReflectionStatus(Authentication authentication) {
        return aiReflectionService.getStatus(memberId(authentication));
    }

    @GetMapping(ApiPath.V1_REFLECTIONS)
    public BaseResponse<List<AiReflectionResponse>> getReflections(Authentication authentication) {
        return aiReflectionService.getAll(memberId(authentication));
    }

    /** Reflection of the last full week; needs the AI_REFLECTION consent. */
    @PostMapping(ApiPath.V1_REFLECTIONS)
    public ResponseEntity<BaseResponse<AiReflectionResponse>> createReflection(Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiReflectionService.create(memberId(authentication)));
    }

    @DeleteMapping(ApiPath.V1_REFLECTIONS + ApiPath.REFLECTION_ID)
    public BaseResponse<Void> deleteReflection(@PathVariable String reflectionId, Authentication authentication) {
        return aiReflectionService.delete(memberId(authentication), reflectionId);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
