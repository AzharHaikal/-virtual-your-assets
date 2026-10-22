package com.vyra.be_virtual_your_assets.filter;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberToken;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.be_virtual_your_assets.security.filter.AuthTokenFilter;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenFilterTest {

    @Mock private MemberTokenRepository memberTokenRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private AuthTokenFilter authTokenFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest memberRequest(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return request;
    }

    @Test
    void adminPath_shouldBeLeftToAdminTokenFilter() throws Exception {
        MockHttpServletRequest request = memberRequest("/api/v1/admin/dashboard/summary", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(memberTokenRepository, never()).findByAccessToken(org.mockito.ArgumentMatchers.any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void whitelistedPath_shouldPassWithoutToken() throws Exception {
        MockHttpServletRequest request = memberRequest("/api/v1/auth/login", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void missingAuthorizationHeader_shouldReturn401() throws Exception {
        MockHttpServletRequest request = memberRequest("/api/v1/member", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void nonBearerAuthorizationHeader_shouldReturn401() throws Exception {
        MockHttpServletRequest request = memberRequest("/api/v1/member", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unknownToken_shouldReturn401() throws Exception {
        when(memberTokenRepository.findByAccessToken("unknown")).thenReturn(Optional.empty());
        MockHttpServletRequest request = memberRequest("/api/v1/member", "Bearer unknown");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("Invalid Token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void expiredToken_shouldReturn401() throws Exception {
        MemberToken token = new MemberToken();
        token.setUserId("member-1");
        token.setAccessToken("expired");
        token.setAccessTokenExpiredAt(LocalDateTime.now().minusMinutes(1));
        when(memberTokenRepository.findByAccessToken("expired")).thenReturn(Optional.of(token));
        MockHttpServletRequest request = memberRequest("/api/v1/member", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo("Access Token Expired");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validTokenOfSuspendedMember_shouldReturn401() throws Exception {
        MemberToken token = new MemberToken();
        token.setUserId("member-1");
        token.setAccessToken("valid");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(memberTokenRepository.findByAccessToken("valid")).thenReturn(Optional.of(token));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(memberWith(ActiveStatus.SUSPENDED)));
        MockHttpServletRequest request = memberRequest("/api/v1/member", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void validTokenOfDeletedOrMissingMember_shouldReturn401() throws Exception {
        MemberToken token = new MemberToken();
        token.setUserId("ghost");
        token.setAccessToken("valid");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(memberTokenRepository.findByAccessToken("valid")).thenReturn(Optional.of(token));
        when(memberRepository.findById("ghost")).thenReturn(Optional.empty());
        MockHttpServletRequest request = memberRequest("/api/v1/member", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    private static Member memberWith(ActiveStatus status) {
        Member member = new Member();
        member.setId("member-1");
        member.setStatus(status);
        return member;
    }

    @Test
    void validToken_shouldAuthenticateMemberAndContinueChain() throws Exception {
        MemberToken token = new MemberToken();
        token.setUserId("member-1");
        token.setAccessToken("valid");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(memberTokenRepository.findByAccessToken("valid")).thenReturn(Optional.of(token));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(memberWith(ActiveStatus.ACTIVE)));
        MockHttpServletRequest request = memberRequest("/api/v1/member", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getMemberId()).isEqualTo("member-1");
        verify(filterChain).doFilter(request, response);
    }
}
