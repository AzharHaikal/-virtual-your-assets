package com.vyra.be_virtual_your_assets.config;

import com.vyra.be_virtual_your_assets.constant.SecurityConstant;
import com.vyra.be_virtual_your_assets.ratelimit.RateLimitFilter;
import com.vyra.be_virtual_your_assets.security.filter.AdminTokenFilter;
import com.vyra.be_virtual_your_assets.security.filter.AuthTokenFilter;
import com.vyra.be_virtual_your_assets.security.filter.MaintenanceFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;
    private final AdminTokenFilter adminTokenFilter;
    private final RateLimitFilter rateLimitFilter;
    private final MetricsTokenAuthorizationManager metricsTokenAuthorizationManager;
    private final MaintenanceFilter maintenanceFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.sendError(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized"
                                    );
                                }
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(SecurityConstant.WHITE_LIST_URL)
                                .permitAll()
                                .requestMatchers(SecurityConstant.ADMIN_WHITE_LIST_URL)
                                .permitAll()
                                .requestMatchers(SecurityConstant.API_DOCS_URL)
                                .permitAll()
                                .requestMatchers(SecurityConstant.PUBLIC_URL)
                                .permitAll()
                                .requestMatchers(SecurityConstant.METRICS_URL)
                                .access(metricsTokenAuthorizationManager)
                                .anyRequest()
                                .authenticated()
                )
                // AuthTokenFilter authenticates mobile app members (and skips /api/v1/admin/**).
                .addFilterBefore(
                        authTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                // AdminTokenFilter authenticates portal administrators on /api/v1/admin/** only.
                .addFilterBefore(
                        adminTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                // Throttles brute-force-prone endpoints before anything else looks at the request.
                .addFilterBefore(
                        rateLimitFilter,
                        AuthTokenFilter.class
                )
                // Maintenance mode answers member requests with 503 before anything else runs.
                .addFilterBefore(
                        maintenanceFilter,
                        RateLimitFilter.class
                );

        return http.build();
    }
}