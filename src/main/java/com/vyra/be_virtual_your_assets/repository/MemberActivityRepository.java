package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MemberActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MemberActivityRepository extends JpaRepository<MemberActivity, String> {

    // Aktivitas milik member (tanpa event ATTEMPT_* agar daftar tidak duplikat/noisy)
    @Query("""
        SELECT a
        FROM MemberActivity a
        WHERE a.phoneNumber = :phoneNumber
        AND a.event NOT LIKE 'ATTEMPT%'
        ORDER BY a.createdAt DESC
        """)
    Page<MemberActivity> findByPhoneNumber(@Param("phoneNumber") String phoneNumber, Pageable pageable);

    /**
     * No nullable parameters: callers pass a lower-cased LIKE pattern ("%foo%",
     * "%" for none) and a half-open [startDate, endDate) window.
     */
    @Query("""
        SELECT a
        FROM MemberActivity a
        WHERE a.createdAt >= :startDate
        AND a.createdAt < :endDate
        AND (
            LOWER(COALESCE(a.phoneNumber, '')) LIKE :pattern
            OR LOWER(COALESCE(a.description, '')) LIKE :pattern
            OR LOWER(COALESCE(a.event, '')) LIKE :eventPattern
        )
        ORDER BY a.createdAt DESC
        """)
    Page<MemberActivity> search(
            @Param("pattern") String pattern,
            @Param("eventPattern") String eventPattern,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
