package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.AdminToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminTokenRepository extends JpaRepository<AdminToken, String> {
    Optional<AdminToken> findByAccessToken(String accessToken);

    Optional<AdminToken> findByRefreshToken(String refreshToken);

    void deleteByAccessToken(String accessToken);

    void deleteByAdminId(String adminId);
}
