package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MemberActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MemberActivationRepository extends JpaRepository<MemberActivation, MemberActivation.Key> {

    /** Records the event unless it already is; the first time wins. Returns 1 when it was new. */
    @Modifying
    @Query(value = """
        INSERT INTO idp.member_activation (member_id, event, occurred_at)
        VALUES (:memberId, :event, :occurredAt)
        ON CONFLICT (member_id, event) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") String memberId,
                       @Param("event") String event,
                       @Param("occurredAt") LocalDateTime occurredAt);
}
