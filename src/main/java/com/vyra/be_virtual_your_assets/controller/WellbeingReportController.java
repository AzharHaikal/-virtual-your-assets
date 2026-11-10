package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.GoalReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.HabitReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.MoodReportItemResponse;
import com.vyra.be_virtual_your_assets.service.WellbeingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.REPORT_GOAL;
import static com.vyra.be_virtual_your_assets.constant.ApiPath.REPORT_HABIT;
import static com.vyra.be_virtual_your_assets.constant.ApiPath.REPORT_MOOD;

/**
 * Member-level wellbeing reports (mood check-ins with journal notes, habits, goals). SUPER_ADMIN only: the service
 * rejects every other role with 403 and writes each report opened to the audit trail.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_REPORT)
public class WellbeingReportController {

    private final WellbeingReportService wellbeingReportService;

    @GetMapping(REPORT_MOOD)
    public BaseResponse<PageResponse<MoodReportItemResponse>> getMoodReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return wellbeingReportService.getMoodReport(startDate, endDate, keyword, page, size);
    }

    @GetMapping(REPORT_HABIT)
    public BaseResponse<PageResponse<HabitReportItemResponse>> getHabitReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return wellbeingReportService.getHabitReport(startDate, endDate, keyword, page, size);
    }

    @GetMapping(REPORT_GOAL)
    public BaseResponse<PageResponse<GoalReportItemResponse>> getGoalReport(
            @RequestParam(required = false) GoalStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return wellbeingReportService.getGoalReport(status, keyword, page, size);
    }
}
