package com.vyra.be_virtual_your_assets.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RateLimitFilter filter(boolean enabled) {
        return new RateLimitFilter(new InMemoryRateLimiter(), objectMapper, enabled);
    }

    private MockHttpServletRequest request(String method, String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletResponse call(RateLimitFilter f, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        f.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void loginIsLimitedPerClientAddressWith429AndRetryAfter() throws Exception {
        RateLimitFilter f = filter(true);

        for (int i = 0; i < 10; i++) {
            assertThat(call(f, request("POST", "/api/v1/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call(f, request("POST", "/api/v1/auth/login", "10.0.0.1"));

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(blocked.getHeader("Retry-After"))).isBetween(1L, 60L);
        assertThat(blocked.getContentAsString()).contains("\"responseStatus\":\"VYRA-RTL-001\"");
        assertThat(blocked.getContentAsString()).contains("Please try again in");
    }

    @Test
    void anotherClientIsNotAffected() throws Exception {
        RateLimitFilter f = filter(true);
        for (int i = 0; i < 11; i++) call(f, request("POST", "/api/v1/auth/login", "10.0.0.1"));

        assertThat(call(f, request("POST", "/api/v1/auth/login", "10.0.0.2")).getStatus()).isEqualTo(200);
    }

    @Test
    void resendOtpAndForgotPinShareOneBucket() throws Exception {
        RateLimitFilter f = filter(true);
        for (int i = 0; i < 3; i++) call(f, request("POST", "/api/v1/auth/resend-otp", "10.0.0.1"));
        for (int i = 0; i < 2; i++) call(f, request("POST", "/api/v1/auth/forgot-pin", "10.0.0.1"));

        assertThat(call(f, request("POST", "/api/v1/auth/forgot-pin", "10.0.0.1")).getStatus()).isEqualTo(429);
    }

    @Test
    void adminLoginHasItsOwnBucket() throws Exception {
        RateLimitFilter f = filter(true);
        for (int i = 0; i < 11; i++) call(f, request("POST", "/api/v1/auth/login", "10.0.0.1"));

        assertThat(call(f, request("POST", "/api/v1/admin/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
    }

    @Test
    void otherEndpointsAndMethodsAreNeverLimited() throws Exception {
        RateLimitFilter f = filter(true);
        for (int i = 0; i < 100; i++) {
            assertThat(call(f, request("GET", "/api/v1/member/get-member", "10.0.0.1")).getStatus()).isEqualTo(200);
            assertThat(call(f, request("GET", "/api/v1/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void trailingSlashIsTreatedAsTheSameEndpoint() throws Exception {
        RateLimitFilter f = filter(true);
        for (int i = 0; i < 10; i++) call(f, request("POST", "/api/v1/auth/login", "10.0.0.1"));

        assertThat(call(f, request("POST", "/api/v1/auth/login/", "10.0.0.1")).getStatus()).isEqualTo(429);
    }

    @Test
    void canBeSwitchedOff() throws Exception {
        RateLimitFilter f = filter(false);
        for (int i = 0; i < 50; i++) {
            assertThat(call(f, request("POST", "/api/v1/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
        }
    }
}
