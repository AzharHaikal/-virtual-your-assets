package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.home.HomeSummaryResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The member's home screen (the portal's dashboard is {@link DashboardController}). */
@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping(ApiPath.V1_HOME_SUMMARY)
    public BaseResponse<HomeSummaryResponse> getSummary(Authentication authentication) {
        return homeService.getSummary(((CustomUserDetails) authentication.getPrincipal()).getMemberId());
    }
}
