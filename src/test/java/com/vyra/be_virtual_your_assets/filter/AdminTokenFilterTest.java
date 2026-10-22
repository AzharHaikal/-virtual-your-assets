package com.vyra.be_virtual_your_assets.filter;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AdminToken;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.AdminTokenRepository;
import com.vyra.be_virtual_your_assets.security.filter.AdminTokenFilter;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTokenFilterTest {

    @Mock private AdminTokenRepository adminTokenRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private AdminTokenFilter adminTokenFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Non-admin paths are skipped entirely (no token check)
    // =========================================================================
    @Test
    void doFilterInternal_nonAdminPath_shouldPassThroughWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/member/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        adminTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(adminTokenRepository, never()).findByAccessToken(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // =========================================================================
    // Whitelisted admin auth endpoints bypass token check
    // =========================================================================
    @Test
    void doFilterInternal_adminLoginEndpoint_shouldBypassTokenCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        adminTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(adminTokenRepository, never()).findByAccessToken(any());
    }

    // =========================================================================
    // Missing/invalid Authorization header
    // =========================================================================
    @Test
    void doFilterInternal_noAuthorizationHeader_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        MockHttpServletResponse response = new MockHttpServletResponse();

        adminTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_authorizationHeaderWithoutBearerPrefix_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        adminTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // =========================================================================
    // Token not found / expired / orphaned
    // =========================================================================
    @Test
    void doFilterInternal_tokenNotFound_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer unknown-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(adminTokenRepository.findByAccessToken("unknown-token")).thenReturn(Optional.empty());

        adminTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_tokenExpired_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AdminToken token = new AdminToken();
        token.setAdminId("admin-id-001");
        token.setAccessToken("expired-token");
        token.setAccessTokenExpiredAt(LocalDateTime.now().minusMinutes(1));
        when(adminTokenRepository.findByAccessToken("expired-token")).thenReturn(Optional.of(token));

        adminTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_tokenValidButAdminNotFound_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AdminToken token = new AdminToken();
        token.setAdminId("admin-id-001");
        token.setAccessToken("valid-token");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(5));
        when(adminTokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.empty());

        adminTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_validTokenButAdminSuspended_shouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AdminToken token = new AdminToken();
        token.setAdminId("admin-id-001");
        token.setAccessToken("valid-token");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(5));
        Admin admin = new Admin();
        admin.setId("admin-id-001");
        admin.setStatus(ActiveStatus.SUSPENDED);
        when(adminTokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));

        adminTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // =========================================================================
    // Valid token -> authentication set, filter chain continues
    // =========================================================================
    @Test
    void doFilterInternal_validToken_shouldSetAuthenticationAndContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/dashboard/summary");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AdminToken token = new AdminToken();
        token.setAdminId("admin-id-001");
        token.setAccessToken("valid-token");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(5));
        when(adminTokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));

        Admin admin = new Admin();
        admin.setId("admin-id-001");
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setStatus(ActiveStatus.ACTIVE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));

        adminTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomAdminDetails.class);
        CustomAdminDetails principal = (CustomAdminDetails) authentication.getPrincipal();
        assertThat(principal.getAdminId()).isEqualTo("admin-id-001");
        assertThat(principal.getRole()).isEqualTo(AdminRole.SUPER_ADMIN);
    }
}
