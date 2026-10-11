package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.audit.AdminAuditItemResponse;
import com.vyra.be_virtual_your_assets.dto.audit.MemberAuditItemResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.AUDIT_MEMBER;
import static com.vyra.be_virtual_your_assets.constant.ApiPath.AUDIT_USER;

/**
 * Exposes two separate audit trails: one for portal administrator (user)
 * activity, and one for mobile app member activity. The two logs are never
 * merged - they come from AdminActivity and MemberActivity respectively.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_AUDIT)
public class AuditController {

    private final AuditService auditService;

    @GetMapping(AUDIT_USER)
    public BaseResponse<PageResponse<AdminAuditItemResponse>> getUserAuditTrail(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return auditService.getUserAuditTrail(keyword, startDate, endDate, page, size);
    }

    @GetMapping(AUDIT_MEMBER)
    public BaseResponse<PageResponse<MemberAuditItemResponse>> getMemberAuditTrail(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return auditService.getMemberAuditTrail(keyword, startDate, endDate, page, size);
    }
}
