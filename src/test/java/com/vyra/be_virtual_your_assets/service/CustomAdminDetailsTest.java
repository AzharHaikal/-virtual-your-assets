package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAdminDetailsTest {

    private CustomAdminDetails adminDetails;

    private static final String ADMIN_ID = "admin-uuid-001";
    private static final String ACCESS_TOKEN = "access-token-abc123";

    @BeforeEach
    void setUp() {
        adminDetails = new CustomAdminDetails(ADMIN_ID, ACCESS_TOKEN, AdminRole.SUPER_ADMIN);
    }

    // =========================================================================
    // Constructor + Getters
    // =========================================================================
    @Test
    void getAdminId_shouldReturnConstructedAdminId() {
        assertThat(adminDetails.getAdminId()).isEqualTo(ADMIN_ID);
    }

    @Test
    void getAccessToken_shouldReturnConstructedAccessToken() {
        assertThat(adminDetails.getAccessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    void getRole_shouldReturnConstructedRole() {
        assertThat(adminDetails.getRole()).isEqualTo(AdminRole.SUPER_ADMIN);
    }

    // =========================================================================
    // getAuthorities
    // =========================================================================
    @Test
    void getAuthorities_withRole_shouldReturnPrefixedRoleAuthority() {
        Collection<? extends GrantedAuthority> authorities = adminDetails.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_SUPER_ADMIN");
    }

    @Test
    void getAuthorities_nullRole_shouldReturnEmptyList() {
        CustomAdminDetails withoutRole = new CustomAdminDetails(ADMIN_ID, ACCESS_TOKEN, null);

        assertThat(withoutRole.getAuthorities()).isEmpty();
    }

    @Test
    void getAuthorities_viewerRole_shouldReturnViewerAuthority() {
        CustomAdminDetails viewer = new CustomAdminDetails(ADMIN_ID, ACCESS_TOKEN, AdminRole.VIEWER);

        assertThat(viewer.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
    }

    // =========================================================================
    // UserDetails interface methods
    // =========================================================================
    @Test
    void getPassword_shouldReturnNull() {
        assertThat(adminDetails.getPassword()).isNull();
    }

    @Test
    void getUsername_shouldReturnAdminId() {
        assertThat(adminDetails.getUsername()).isEqualTo(ADMIN_ID);
    }

    @Test
    void isAccountNonExpired_shouldReturnTrue() {
        assertThat(adminDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_shouldReturnTrue() {
        assertThat(adminDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_shouldReturnTrue() {
        assertThat(adminDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabled_shouldReturnTrue() {
        assertThat(adminDetails.isEnabled()).isTrue();
    }
}
