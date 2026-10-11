package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.AdminActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminActivityRepository extends JpaRepository<AdminActivity, String> {

    /**
     * No nullable parameters: callers pass a lower-cased LIKE pattern ("%foo%",
     * "%" for none) and a half-open [startDate, endDate) window. Null parameters
     * in JPQL are unreliable on PostgreSQL ("could not determine data type").
     */
    @Query("""
        SELECT a
        FROM AdminActivity a
        WHERE a.createdAt >= :startDate
        AND a.createdAt < :endDate
        AND (
            LOWER(COALESCE(a.adminName, '')) LIKE :pattern
            OR LOWER(COALESCE(a.description, '')) LIKE :pattern
            OR LOWER(COALESCE(a.ipAddress, '')) LIKE :pattern
            OR LOWER(COALESCE(a.event, '')) LIKE :eventPattern
        )
        ORDER BY a.createdAt DESC
        """)
    Page<AdminActivity> search(
            @Param("pattern") String pattern,
            @Param("eventPattern") String eventPattern,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
