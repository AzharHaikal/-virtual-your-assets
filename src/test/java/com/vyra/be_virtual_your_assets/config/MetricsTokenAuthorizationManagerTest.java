package com.vyra.be_virtual_your_assets.config;

import com.vyra.be_virtual_your_assets.constant.SecurityConstant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsTokenAuthorizationManagerTest {

    @Test
    void onlyTheConfiguredToken_opensTheMetrics() {
        MetricsTokenAuthorizationManager manager = new MetricsTokenAuthorizationManager("s3cret");

        assertThat(manager.check(() -> null, context("s3cret")).isGranted()).isTrue();
        assertThat(manager.check(() -> null, context("wrong")).isGranted()).isFalse();
        assertThat(manager.check(() -> null, context(null)).isGranted()).isFalse();
    }

    @Test
    void withoutAConfiguredToken_nobodyCanReadThem() {
        MetricsTokenAuthorizationManager manager = new MetricsTokenAuthorizationManager("");

        assertThat(manager.matches("")).isFalse();
        assertThat(manager.check(() -> null, context("anything")).isGranted()).isFalse();
    }

    private static RequestAuthorizationContext context(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", SecurityConstant.METRICS_URL);
        if (token != null) {
            request.addHeader(SecurityConstant.METRICS_TOKEN_HEADER, token);
        }
        return new RequestAuthorizationContext(request);
    }
}
