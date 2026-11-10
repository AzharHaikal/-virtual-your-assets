package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAuthorizationServiceTest {

    private final AdminAuthorizationService adminAuthorizationService = new AdminAuthorizationService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // getCurrentAdmin
    // =========================================================================
    @Test
    void getCurrentAdmin_validAdminPrincipal_shouldReturnAdminDetails() {
        CustomAdminDetails adminDetails = new CustomAdminDetails("admin-id-001", "token", AdminRole.SUPER_ADMIN);
        authenticateAs(adminDetails);

        CustomAdminDetails result = adminAuthorizationService.getCurrentAdmin();

        assertThat(result.getAdminId()).isEqualTo("admin-id-001");
        assertThat(result.getRole()).isEqualTo(AdminRole.SUPER_ADMIN);
    }

    @Test
    void getCurrentAdmin_noAuthentication_shouldThrowForbiddenException() {
        SecurityContextHolder.clearContext();

        ForbiddenException ex = assertThrows(ForbiddenException.class, adminAuthorizationService::getCurrentAdmin);

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED);
    }

    @Test
    void getCurrentAdmin_principalIsNotCustomAdminDetails_shouldThrowForbiddenException() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("some-other-principal", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ForbiddenException ex = assertThrows(ForbiddenException.class, adminAuthorizationService::getCurrentAdmin);

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED);
    }

    // =========================================================================
    // requirePermission
    // =========================================================================
    @Test
    void requirePermission_superAdminHasAllPermissions_shouldNotThrow() {
        authenticateAs(new CustomAdminDetails("admin-id-001", "token", AdminRole.SUPER_ADMIN));

        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_MANAGE);
        // no exception = pass
    }

    @Test
    void requirePermission_viewerLacksUserManage_shouldThrowForbiddenException() {
        authenticateAs(new CustomAdminDetails("admin-id-002", "token", AdminRole.VIEWER));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED);
    }

    @Test
    void requirePermission_operatorHasMemberManageButNotUserManage_shouldBehaveAccordingly() {
        authenticateAs(new CustomAdminDetails("admin-id-003", "token", AdminRole.OPERATOR));

        // OPERATOR is allowed to manage members
        adminAuthorizationService.requirePermission(AdminPermission.MEMBER_MANAGE);

        // but OPERATOR cannot manage other admin accounts
        assertThrows(ForbiddenException.class,
                () -> adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE));
    }

    @Test
    void requirePermission_administratorHasUserManageButNotSettingsManage_shouldBehaveAccordingly() {
        authenticateAs(new CustomAdminDetails("admin-id-004", "token", AdminRole.ADMINISTRATOR));

        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);

        assertThrows(ForbiddenException.class,
                () -> adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_MANAGE));
    }

    // =========================================================================
    // member transaction access (SUPER_ADMIN only, never switched off)
    // =========================================================================
    @Test
    void requireMemberTransactionAccess_superAdmin_isAlwaysAllowed() {
        authenticateAs(new CustomAdminDetails("admin-id-001", "token", AdminRole.SUPER_ADMIN));

        assertDoesNotThrow(adminAuthorizationService::requireMemberTransactionAccess);
        assertThat(adminAuthorizationService.canViewMemberTransactions()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AdminRole.class, names = {"ADMINISTRATOR", "OPERATOR", "VIEWER"})
    void requireMemberTransactionAccess_otherRoles_shouldAlwaysThrow(AdminRole role) {
        authenticateAs(new CustomAdminDetails("admin-id-005", "token", role));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                adminAuthorizationService::requireMemberTransactionAccess);

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED);
        assertThat(adminAuthorizationService.canViewMemberTransactions()).isFalse();
    }

    @Test
    void effectivePermissions_superAdmin_holdsEveryPermission() {
        assertThat(adminAuthorizationService.effectivePermissions(AdminRole.SUPER_ADMIN))
                .containsExactlyInAnyOrder(AdminPermission.values());
    }

    @ParameterizedTest
    @EnumSource(value = AdminRole.class, names = {"ADMINISTRATOR", "OPERATOR", "VIEWER"})
    void effectivePermissions_otherRoles_neverIncludeMemberDataPermissions(AdminRole role) {
        assertThat(adminAuthorizationService.effectivePermissions(role))
                .doesNotContain(AdminPermission.MEMBER_TRANSACTION_VIEW, AdminPermission.MEMBER_WELLBEING_VIEW);
    }

    // =========================================================================
    // bulk export (REPORT_EXPORT)
    // =========================================================================
    @ParameterizedTest
    @EnumSource(value = AdminRole.class, names = {"SUPER_ADMIN", "ADMINISTRATOR"})
    void requireExportIfBulk_rolesWithExport_mayDownloadBigPages(AdminRole role) {
        authenticateAs(new CustomAdminDetails("admin-id-006", "token", role));

        assertDoesNotThrow(() -> adminAuthorizationService.requireExportIfBulk(500));
    }

    @ParameterizedTest
    @EnumSource(value = AdminRole.class, names = {"OPERATOR", "VIEWER"})
    void requireExportIfBulk_rolesWithoutExport_onlyGetScreenSizedPages(AdminRole role) {
        authenticateAs(new CustomAdminDetails("admin-id-007", "token", role));

        assertDoesNotThrow(() -> adminAuthorizationService.requireExportIfBulk(AdminAuthorizationService.BULK_PAGE_SIZE));
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> adminAuthorizationService.requireExportIfBulk(AdminAuthorizationService.BULK_PAGE_SIZE + 1));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED);
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private void authenticateAs(CustomAdminDetails adminDetails) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
