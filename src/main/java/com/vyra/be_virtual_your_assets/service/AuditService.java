package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.audit.AdminAuditItemResponse;
import com.vyra.be_virtual_your_assets.dto.audit.MemberAuditItemResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AdminActivity;
import com.vyra.be_virtual_your_assets.entity.MemberActivity;
import com.vyra.be_virtual_your_assets.repository.AdminActivityRepository;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.MemberActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides the two audit trail listings shown in the portal: administrator
 * (user) activity and mobile app member activity. Reads only - audit entries
 * are written by AdminActivityService and MemberActivityService respectively.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {

    private static final int MAX_PAGE_SIZE = 500;
    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 1, 1, 0, 0);

    private final AdminActivityRepository adminActivityRepository;
    private final MemberActivityRepository memberActivityRepository;
    private final AdminRepository adminRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public BaseResponse<PageResponse<AdminAuditItemResponse>> getUserAuditTrail(String keyword, LocalDate startDate, LocalDate endDate, int page, int size) {
        adminAuthorizationService.requirePermission(AdminPermission.AUDIT_VIEW);

        String pattern = likePattern(keyword);
        Page<AdminActivity> activityPage = adminActivityRepository.search(
                pattern,
                eventPattern(pattern),
                startOf(startDate),
                endExclusive(endDate),
                pageRequest(page, size)
        );

        Map<String, String> adminNames = new HashMap<>();
        List<AdminAuditItemResponse> items = activityPage.getContent()
                .stream()
                .map(a -> toAdminAuditItem(a, adminNames))
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.AUDIT_USER_LIST_SUCCESS.getCode(),
                ErrorConstant.AUDIT_USER_LIST_SUCCESS.getMessage(),
                PageResponse.from(activityPage, items)
        );
    }

    public BaseResponse<PageResponse<MemberAuditItemResponse>> getMemberAuditTrail(String keyword, LocalDate startDate, LocalDate endDate, int page, int size) {
        adminAuthorizationService.requirePermission(AdminPermission.AUDIT_VIEW);

        String pattern = likePattern(keyword);
        Page<MemberActivity> activityPage = memberActivityRepository.search(
                pattern,
                eventPattern(pattern),
                startOf(startDate),
                endExclusive(endDate),
                pageRequest(page, size)
        );

        List<MemberAuditItemResponse> items = activityPage.getContent()
                .stream()
                .map(this::toMemberAuditItem)
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.AUDIT_MEMBER_LIST_SUCCESS.getCode(),
                ErrorConstant.AUDIT_MEMBER_LIST_SUCCESS.getMessage(),
                PageResponse.from(activityPage, items)
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    /** "%keyword%" lower-cased, or "%" (match everything) when blank. */
    private static String likePattern(String keyword) {
        return StringUtils.isBlank(keyword) ? "%" : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    /** The portal shows events as "SUCCESS LOGIN"; stored values use underscores. */
    private static String eventPattern(String pattern) {
        return pattern.replace(' ', '_');
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date != null ? date.atStartOfDay() : MIN_DATE;
    }

    /** First instant AFTER the selected end date, used with a strict "<". */
    private static LocalDateTime endExclusive(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : MAX_DATE;
    }

    private AdminAuditItemResponse toAdminAuditItem(AdminActivity activity, Map<String, String> nameCache) {
        AdminAuditItemResponse response = new AdminAuditItemResponse();
        response.setId(activity.getId());
        response.setAdminId(activity.getAdminId());
        response.setAdminName(displayName(activity, nameCache));
        response.setEvent(activity.getEvent());
        response.setDescription(activity.getDescription());
        response.setIpAddress(activity.getIpAddress());
        response.setDevice(activity.getDevice());
        response.setCreatedAt(activity.getCreatedAt());
        return response;
    }

    /**
     * Older rows were written with the admin id in the name column. Show the
     * admin's real name for those instead of a raw UUID.
     */
    private String displayName(AdminActivity activity, Map<String, String> nameCache) {
        String name = activity.getAdminName();
        String adminId = activity.getAdminId();
        if (StringUtils.isBlank(adminId) || !adminId.equals(name)) {
            return name;
        }
        return nameCache.computeIfAbsent(adminId, id ->
                adminRepository.findById(id).map(Admin::getName).orElse(name));
    }

    private MemberAuditItemResponse toMemberAuditItem(MemberActivity activity) {
        MemberAuditItemResponse response = new MemberAuditItemResponse();
        response.setId(activity.getId());
        response.setPhoneNumber(activity.getPhoneNumber());
        response.setEvent(activity.getEvent());
        response.setDescription(activity.getDescription());
        response.setCreatedAt(activity.getCreatedAt());
        return response;
    }
}
