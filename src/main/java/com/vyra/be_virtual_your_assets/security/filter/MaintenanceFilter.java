package com.vyra.be_virtual_your_assets.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.service.SystemSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Maintenance mode ({@code maintenance.enabled}): every member API answers 503 with the maintenance
 * message, so the app shows its maintenance screen. The admin portal, the health check, the public
 * status and the legal documents keep working.
 */
@Component
@RequiredArgsConstructor
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final List<String> ALWAYS_OPEN = List.of(
            "/api/v1/admin/**",
            "/api/v1/status",
            "/api/v1/legal/**",
            "/actuator/**"
    );

    private final SystemSettings systemSettings;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/") || ALWAYS_OPEN.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "300");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new BaseResponse<>(
                ErrorConstant.MAINTENANCE_MODE.getCode(), message(systemSettings), null));
    }

    /** The portal's message, or the default text. */
    public static String message(SystemSettings settings) {
        return StringUtils.defaultIfBlank(settings.text(SystemConfigKey.MAINTENANCE_MESSAGE),
                ErrorConstant.MAINTENANCE_MODE.getMessage());
    }
}
