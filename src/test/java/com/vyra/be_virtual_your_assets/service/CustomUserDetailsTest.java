package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    private CustomUserDetails userDetails;

    private static final String MEMBER_ID = "member-uuid-001";
    private static final String ACCESS_TOKEN = "access-token-abc123";

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(MEMBER_ID, ACCESS_TOKEN);
    }

    // =========================================================================
    // Constructor + Getters
    // =========================================================================
    @Test
    void getMemberId_shouldReturnConstructedMemberId() {
        assertThat(userDetails.getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void getAccessToken_shouldReturnConstructedAccessToken() {
        assertThat(userDetails.getAccessToken()).isEqualTo(ACCESS_TOKEN);
    }

    // =========================================================================
    // UserDetails interface methods
    // =========================================================================
    @Test
    void getAuthorities_shouldReturnEmptyList() {
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    void getPassword_shouldReturnNull() {
        assertThat(userDetails.getPassword()).isNull();
    }

    @Test
    void getUsername_shouldReturnMemberId() {
        assertThat(userDetails.getUsername()).isEqualTo(MEMBER_ID);
    }

    @Test
    void isAccountNonExpired_shouldReturnTrue() {
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_shouldReturnTrue() {
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_shouldReturnTrue() {
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabled_shouldReturnTrue() {
        assertThat(userDetails.isEnabled()).isTrue();
    }
}
