package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.insight.WeeklyInsightResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Summaries across modules. Only for the member themself. */
@RestController
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    /** date = any day of the week (yyyy-MM-dd); default today in the member's zone. */
    @GetMapping(ApiPath.V1_INSIGHTS_WEEKLY)
    public BaseResponse<WeeklyInsightResponse> getWeekly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return insightService.getWeekly(((CustomUserDetails) authentication.getPrincipal()).getMemberId(), date);
    }
}
