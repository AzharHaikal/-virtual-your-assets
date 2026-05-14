package com.vyra.virtual_your_assets.security.filter;

import com.vyra.virtual_your_assets.constant.SecurityConstant;
import com.vyra.virtual_your_assets.entity.MemberToken;
import com.vyra.virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.virtual_your_assets.security.model.CustomUserDetails;
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

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final MemberTokenRepository memberTokenRepository;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String accessToken = authorizationHeader.substring(7);

        MemberToken memberToken = memberTokenRepository.findByAccessToken(accessToken).orElse(null);
        if (memberToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
            return;
        }

        if (memberToken.getAccessTokenExpiredAt().isBefore(LocalDateTime.now())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Token Expired");
            return;
        }

        CustomUserDetails userDetails = new CustomUserDetails(memberToken.getMemberId(), memberToken.getAccessToken());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String requestPath) {
        return Arrays.stream(SecurityConstant.WHITE_LIST_URL).anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }
}