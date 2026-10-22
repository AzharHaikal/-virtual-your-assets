package com.vyra.be_virtual_your_assets.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Per-client-address throttling of the unauthenticated, brute-force-prone endpoints
 * (login, OTP, PIN reset, registration, token refresh). Runs before authentication.
 * Counters are shared by all backend instances through {@code DatabaseRateLimiter}.
 * The client address is {@code request.getRemoteAddr()}, which is the real client when the
 * container is configured with {@code server.forward-headers-strategy=native}.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    /** One throttling rule: requests of a bucket allowed per window and client. */
    public record Rule(String bucket, int limit, Duration window) {}

    private static final Duration MINUTE = Duration.ofMinutes(1);

    static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("POST /api/v1/auth/login", new Rule("login", 10, MINUTE)),
            Map.entry("POST /api/v1/admin/auth/login", new Rule("admin-login", 10, MINUTE)),
            Map.entry("POST /api/v1/auth/verify-otp", new Rule("otp-verify", 10, MINUTE)),
            Map.entry("POST /api/v1/auth/resend-otp", new Rule("otp-send", 5, MINUTE)),
            Map.entry("POST /api/v1/auth/forgot-pin", new Rule("otp-send", 5, MINUTE)),
            Map.entry("POST /api/v1/auth/reset-pin", new Rule("pin-reset", 5, MINUTE)),
            Map.entry("POST /api/v1/auth/register", new Rule("register", 10, Duration.ofMinutes(10))),
            Map.entry("POST /api/v1/auth/refresh-token", new Rule("refresh", 30, MINUTE)),
            Map.entry("POST /api/v1/admin/auth/refresh-token", new Rule("admin-refresh", 30, MINUTE))
    );

    private final RateLimiter limiter;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    @Autowired
    public RateLimitFilter(RateLimiter limiter,
                           ObjectMapper objectMapper,
                           @Value("${vyra.security.rate-limit.enabled:true}") boolean enabled) {
        this.limiter = limiter;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || ruleFor(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = ruleFor(request);
        String key = rule.bucket() + "|" + request.getRemoteAddr();

        long retryAfter = limiter.tryAcquire(key, rule.limit(), rule.window().toMillis());
        if (retryAfter > 0) {
            log.warn("Rate limit exceeded. bucket: {} client: {}", rule.bucket(), request.getRemoteAddr());
            TooManyRequestsException ex = new TooManyRequestsException(ErrorConstant.TOO_MANY_REQUESTS, retryAfter);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    new BaseResponse<Void>(ex.getErrorConstant().getCode(), ex.getMessage(), null));
            return;
        }
        chain.doFilter(request, response);
    }

    private static Rule ruleFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return RULES.get(request.getMethod() + " " + path);
    }
}
