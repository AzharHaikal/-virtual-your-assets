package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    /** Atomically claims the key: 1 when this request owns it, 0 when it already exists. */
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO transaction_finance.idempotency_key (id, member_id, request_hash, created_at)
        VALUES (:id, :memberId, :requestHash, :createdAt)
        ON CONFLICT (id) DO NOTHING
        """, nativeQuery = true)
    int reserve(@Param("id") String id,
                @Param("memberId") String memberId,
                @Param("requestHash") String requestHash,
                @Param("createdAt") LocalDateTime createdAt);

    @Transactional
    @Modifying
    @Query("UPDATE IdempotencyKey k SET k.responseBody = :body WHERE k.id = :id")
    int complete(@Param("id") String id, @Param("body") String body);

    @Transactional
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.memberId = :memberId AND k.createdAt < :before")
    int deleteExpired(@Param("memberId") String memberId, @Param("before") LocalDateTime before);
}
