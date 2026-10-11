package com.vyra.be_virtual_your_assets.security.filter;

import com.vyra.be_virtual_your_assets.constant.SecurityConstant;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AdminToken;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.AdminTokenRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Authenticates requests under the {@code /api/v1/admin/**} path using an
 * {@link AdminToken}. This filter is completely independent from
 * {@link AuthTokenFilter}, which only authenticates mobile app member
 * requests via {@code MemberToken}. The two token systems never overlap.
 */
@Component
@RequiredArgsConstructor
public class AdminTokenFilter extends OncePerRequestFilter {

    private final AdminTokenRepository adminTokenRepository;
    private final AdminRepository adminRepository;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (!isAdminPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAdminWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String accessToken = authorizationHeader.substring(7);

        AdminToken adminToken = adminTokenRepository.findByAccessToken(accessToken).orElse(null);
        if (adminToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        if (adminToken.getAccessTokenExpiredAt().isBefore(LocalDateTime.now())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Token Expired");
            return;
        }

        Admin admin = adminRepository.findById(adminToken.getAdminId()).orElse(null);
        if (admin == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        CustomAdminDetails adminDetails = new CustomAdminDetails(admin.getId(), adminToken.getAccessToken(), admin.getRole());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isAdminPath(String requestPath) {
        return pathMatcher.match(SecurityConstant.ADMIN_PATH_PREFIX, requestPath);
    }

    private boolean isAdminWhitelisted(String requestPath) {
        return Arrays.stream(SecurityConstant.ADMIN_WHITE_LIST_URL).anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }
}
