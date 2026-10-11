package com.vyra.be_virtual_your_assets.security.model;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Security principal for an authenticated portal administrator.
 * Kept fully separate from {@link CustomUserDetails}, which represents
 * an authenticated mobile app member.
 */
public class CustomAdminDetails implements UserDetails {

    private final String adminId;
    private final String accessToken;
    private final AdminRole role;

    public CustomAdminDetails(String adminId, String accessToken, AdminRole role) {
        this.adminId = adminId;
        this.accessToken = accessToken;
        this.role = role;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public AdminRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return adminId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
