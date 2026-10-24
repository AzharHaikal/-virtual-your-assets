package com.vyra.be_virtual_your_assets.config;

import com.vyra.be_virtual_your_assets.constant.SecurityConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Supplier;

/**
 * Lets the metrics scraper read /actuator/prometheus with a shared token (header X-Metrics-Token).
 * Without a configured token ({@code vyra.metrics.token}) nobody can read the metrics.
 */
@Component
public class MetricsTokenAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final byte[] token;

    public MetricsTokenAuthorizationManager(@Value("${vyra.metrics.token:}") String token) {
        this.token = StringUtils.isBlank(token) ? null : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(matches(context.getRequest().getHeader(SecurityConstant.METRICS_TOKEN_HEADER)));
    }

    boolean matches(String header) {
        if (token == null || header == null) {
            return false;
        }
        return MessageDigest.isEqual(token, header.getBytes(StandardCharsets.UTF_8));
    }
}
