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
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.AdminActivityRepository;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.MemberActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AdminActivityRepository adminActivityRepository;
    @Mock private MemberActivityRepository memberActivityRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Spy private TimeService timeService = new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.AUDIT_VIEW);
    }

    // =========================================================================
    // getUserAuditTrail
    // =========================================================================
    @Test
    void getUserAuditTrail_shouldReturnPagedAdminActivities() {
        AdminActivity activity = new AdminActivity();
        activity.setId("activity-001");
        activity.setAdminId("admin-id-001");
        activity.setAdminName("Budi Admin");
        activity.setEvent("SUCCESS_LOGIN");
        activity.setDescription("Admin successfully logged in");
        activity.setIpAddress("127.0.0.1");
        activity.setDevice("Chrome");
        activity.setCreatedAt(LocalDateTime.now());

        Page<AdminActivity> page = new PageImpl<>(List.of(activity), PageRequest.of(0, 10), 1);
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        BaseResponse<PageResponse<AdminAuditItemResponse>> result =
                auditService.getUserAuditTrail("budi", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 18), 0, 10);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.AUDIT_USER_LIST_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getAdminName()).isEqualTo("Budi Admin");
    }

    @Test
    void getUserAuditTrail_noFilters_shouldMatchEverythingWithoutNullParameters() {
        Page<AdminActivity> page = new PageImpl<>(List.of());
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        auditService.getUserAuditTrail(null, null, null, 0, 10);

        verify(adminActivityRepository).search(
                eq("%"), eq("%"),
                eq(LocalDateTime.of(1970, 1, 1, 0, 0)),
                eq(LocalDateTime.of(9999, 1, 1, 0, 0)),
                any(Pageable.class)
        );
    }

    @Test
    void getUserAuditTrail_keywordWithSpaces_shouldMatchEventNamesWithUnderscores() {
        Page<AdminActivity> page = new PageImpl<>(List.of());
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        auditService.getUserAuditTrail("  Success Login ", null, null, 0, 10);

        verify(adminActivityRepository).search(
                eq("%success login%"), eq("%success_login%"), any(), any(), any(Pageable.class));
    }

    @Test
    void getUserAuditTrail_endDate_shouldBeInclusiveOfWholeDay() {
        Page<AdminActivity> page = new PageImpl<>(List.of());
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        auditService.getUserAuditTrail(null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 0, 10);

        verify(adminActivityRepository).search(
                any(), any(),
                // days in the default zone (Asia/Jakarta, UTC+7) as UTC instants
                eq(LocalDateTime.of(2026, 5, 31, 17, 0)),
                eq(LocalDateTime.of(2026, 6, 5, 17, 0)),   // exclusive upper bound
                any(Pageable.class)
        );
    }

    @Test
    void getUserAuditTrail_legacyRowWithIdAsName_shouldShowRealAdminName() {
        AdminActivity activity = new AdminActivity();
        activity.setId("activity-009");
        activity.setAdminId("admin-id-001");
        activity.setAdminName("admin-id-001");       // old rows stored the id here
        activity.setEvent("CREATE_ADMIN");
        activity.setCreatedAt(LocalDateTime.now());

        Admin admin = new Admin();
        admin.setName("Budi Admin");
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activity)));

        BaseResponse<PageResponse<AdminAuditItemResponse>> result =
                auditService.getUserAuditTrail(null, null, null, 0, 10);

        assertThat(result.getData().getContent().get(0).getAdminName()).isEqualTo("Budi Admin");
    }

    @Test
    void getUserAuditTrail_sizeAboveLimit_shouldBeClamped() {
        Page<AdminActivity> page = new PageImpl<>(List.of());
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        auditService.getUserAuditTrail(null, null, null, -3, 100000);

        verify(adminActivityRepository).search(any(), any(), any(), any(), eq(PageRequest.of(0, 500)));
    }

    @Test
    void getUserAuditTrail_noPermission_shouldThrowForbiddenException() {
        doThrow(new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED))
                .when(adminAuthorizationService).requirePermission(AdminPermission.AUDIT_VIEW);

        assertThrows(ForbiddenException.class,
                () -> auditService.getUserAuditTrail(null, null, null, 0, 10));
        verify(adminActivityRepository, never()).search(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // getMemberAuditTrail
    // =========================================================================
    @Test
    void getMemberAuditTrail_shouldReturnPagedMemberActivities() {
        MemberActivity activity = new MemberActivity();
        activity.setId("activity-002");
        activity.setPhoneNumber("628123456789");
        activity.setEvent("SUCCESS_LOGIN");
        activity.setDescription("Member successfully logged in");
        activity.setCreatedAt(LocalDateTime.now());

        Page<MemberActivity> page = new PageImpl<>(List.of(activity), PageRequest.of(0, 10), 1);
        when(memberActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        BaseResponse<PageResponse<MemberAuditItemResponse>> result =
                auditService.getMemberAuditTrail(null, null, null, 0, 10);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.AUDIT_MEMBER_LIST_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getPhoneNumber()).isEqualTo("628123456789");
    }

    @Test
    void getMemberAuditTrail_withDateRange_shouldUseStartOfDayAndExclusiveNextDay() {
        Page<MemberActivity> page = new PageImpl<>(List.of());
        when(memberActivityRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        auditService.getMemberAuditTrail("Keyword", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 0, 10);

        verify(memberActivityRepository).search(
                eq("%keyword%"),
                eq("%keyword%"),
                // days in the default zone (Asia/Jakarta, UTC+7) as UTC instants
                eq(LocalDateTime.of(2026, 5, 31, 17, 0)),
                eq(LocalDateTime.of(2026, 6, 5, 17, 0)),
                any(Pageable.class)
        );
    }


    @Test
    void getUserAuditTrail_rowWithoutAdminId_shouldKeepStoredName() {
        AdminActivity activity = new AdminActivity();
        activity.setId("activity-010");
        activity.setAdminId(null);
        activity.setAdminName("ghost@example.com");
        activity.setEvent("FAILED_LOGIN");
        activity.setCreatedAt(LocalDateTime.now());
        when(adminActivityRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activity)));

        BaseResponse<PageResponse<AdminAuditItemResponse>> result =
                auditService.getUserAuditTrail(null, null, null, 0, 10);

        assertThat(result.getData().getContent().get(0).getAdminName()).isEqualTo("ghost@example.com");
        verify(adminRepository, never()).findById(any());
    }
}
