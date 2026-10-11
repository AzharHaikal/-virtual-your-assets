package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberDetailResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberListItemResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberStatusUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberTransactionResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

/**
 * Member management endpoints for the portal: list/search members, view detail
 * with transaction history, update status (suspend/activate), and delete.
 * This is read/management access only - it never re-implements member
 * registration/auth logic, which stays exclusively in AuthenticationService.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_MEMBER)
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping(ADMIN_MEMBER_LIST)
    public BaseResponse<PageResponse<AdminMemberListItemResponse>> getMemberList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ActiveStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminMemberService.getMemberList(keyword, status, page, size);
    }

    @GetMapping(ADMIN_MEMBER_DETAIL)
    public BaseResponse<AdminMemberDetailResponse> getMemberDetail(@PathVariable String memberId) {
        return adminMemberService.getMemberDetail(memberId);
    }

    @GetMapping(ADMIN_MEMBER_TRANSACTIONS)
    public BaseResponse<PageResponse<AdminMemberTransactionResponse>> getMemberTransactions(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminMemberService.getMemberTransactions(memberId, page, size);
    }

    @PatchMapping(ADMIN_MEMBER_STATUS)
    public BaseResponse<Void> updateMemberStatus(
            @PathVariable String memberId,
            @Valid @RequestBody AdminMemberStatusUpdateRequest request,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminMemberService.updateMemberStatus(memberId, request, currentAdmin);
    }

    @DeleteMapping(ADMIN_MEMBER_DELETE)
    public BaseResponse<Void> deleteMember(@PathVariable String memberId, Authentication authentication) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminMemberService.deleteMember(memberId, currentAdmin);
    }
}
