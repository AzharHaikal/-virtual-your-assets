package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.metrics.ProductMetricsResponse;
import com.vyra.be_virtual_your_assets.dto.metrics.SystemStatusResponse;
import com.vyra.be_virtual_your_assets.service.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Product metrics and service status of the portal (DASHBOARD_VIEW). */
@RestController
@RequiredArgsConstructor
public class AdminMetricsController {

    private final ProductMetricsService productMetricsService;

    /** days = 7..90 (default 30). */
    @GetMapping(ApiPath.V1_ADMIN_METRICS)
    public BaseResponse<ProductMetricsResponse> getMetrics(@RequestParam(required = false) Integer days) {
        return productMetricsService.getMetrics(days);
    }

    @GetMapping(ApiPath.V1_ADMIN_SYSTEM_STATUS)
    public BaseResponse<SystemStatusResponse> getStatus() {
        return productMetricsService.getStatus();
    }
}
