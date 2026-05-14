package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.MemberToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberTokenRepository extends JpaRepository<MemberToken, UUID> {
    Optional<MemberToken> findByAccessToken(String token);

    Optional<MemberToken> findByRefreshToken(String refreshToken);

    void deleteByAccessToken(String accessToken);
}
