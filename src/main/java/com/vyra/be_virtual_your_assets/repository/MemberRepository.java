package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByPhoneNumber(String phoneNumber);

    Optional<Member> findByEmailIgnoreCase(String email);

    // Atomic lock-out bookkeeping (always run in their own transaction by LockoutService).
    @Modifying
    @Query("UPDATE Member m SET m.failedPinAttempts = COALESCE(m.failedPinAttempts, 0) + 1 WHERE m.id = :id")
    int incrementFailedPinAttempts(@Param("id") String id);

    @Query("SELECT COALESCE(m.failedPinAttempts, 0) FROM Member m WHERE m.id = :id")
    int getFailedPinAttempts(@Param("id") String id);

    @Modifying
    @Query("UPDATE Member m SET m.pinLockedUntil = :until WHERE m.id = :id")
    int lockPin(@Param("id") String id, @Param("until") LocalDateTime until);

    @Modifying
    @Query("UPDATE Member m SET m.failedPinAttempts = 0, m.pinLockedUntil = NULL WHERE m.id = :id")
    int clearPinLock(@Param("id") String id);


    long countByStatus(ActiveStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Member> findTop5ByOrderByCreatedAtDesc();

    List<Member> findByIdIn(java.util.Collection<String> ids);

    @Query("""
        SELECT m
        FROM Member m
        WHERE m.deletedAt IS NULL
        AND (:status IS NULL OR m.status = :status)
        AND (
            :keyword IS NULL
            OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR m.phoneNumber LIKE CONCAT('%', :keyword, '%')
        )
        ORDER BY m.createdAt DESC
        """)
    Page<Member> searchMembers(
            @Param("keyword") String keyword,
            @Param("status") ActiveStatus status,
            Pageable pageable
    );
}

