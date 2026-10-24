package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.DevicePushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, String> {

    Optional<DevicePushToken> findByToken(String token);

    List<DevicePushToken> findByMemberId(String memberId);

    @Modifying
    @Query("DELETE FROM DevicePushToken t WHERE t.memberId = :memberId AND t.token = :token")
    int deleteOwned(@Param("memberId") String memberId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM DevicePushToken t WHERE t.memberId = :memberId")
    int deleteAllOfMember(@Param("memberId") String memberId);
}
