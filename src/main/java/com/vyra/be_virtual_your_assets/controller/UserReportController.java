package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.user.CreateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UpdateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UserReportResponse;
import com.vyra.be_virtual_your_assets.entity.UserReportAttachment;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.UserReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reports from members, and the handling queue in the portal. */
@RestController
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService userReportService;

    @PostMapping(ApiPath.V1_USER_REPORTS)
    public ResponseEntity<BaseResponse<UserReportResponse>> create(
            @Valid @RequestBody CreateUserReportRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userReportService.create(memberId(authentication), request));
    }

    @GetMapping(ApiPath.V1_USER_REPORTS)
    public BaseResponse<List<UserReportResponse>> getOwn(Authentication authentication) {
        return userReportService.getOwn(memberId(authentication));
    }

    @GetMapping(ApiPath.V1_ADMIN_USER_REPORTS)
    public BaseResponse<PageResponse<UserReportResponse>> getQueue(
            @RequestParam(required = false) UserReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userReportService.getQueue(status, page, size);
    }

    @GetMapping(ApiPath.V1_ADMIN_USER_REPORTS + ApiPath.ADMIN_USER_REPORT_ID)
    public BaseResponse<UserReportResponse> getDetail(@PathVariable String reportId) {
        return userReportService.getDetail(reportId);
    }

    @GetMapping(ApiPath.V1_ADMIN_USER_REPORTS + ApiPath.ADMIN_USER_REPORT_ATTACHMENT)
    public ResponseEntity<byte[]> getAttachment(@PathVariable String reportId) {
        UserReportAttachment attachment = userReportService.getAttachment(reportId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .cacheControl(CacheControl.noStore())
                .body(attachment.getContent());
    }

    @PatchMapping(ApiPath.V1_ADMIN_USER_REPORTS + ApiPath.ADMIN_USER_REPORT_ID)
    public BaseResponse<UserReportResponse> update(
            @PathVariable String reportId,
            @Valid @RequestBody UpdateUserReportRequest request,
            Authentication authentication
    ) {
        return userReportService.update(reportId, request, (CustomAdminDetails) authentication.getPrincipal());
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
