package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * Creates the notification unless one with the same dedup key exists (from a parallel run or an
     * earlier one). Returns 1 when it was created.
     */
    @Modifying
    @Query(value = """
        INSERT INTO notification.notification (id, member_id, type, title, body, payload, dedup_key, read_at, created_at)
        VALUES (:id, :memberId, :type, :title, :body, CAST(:payload AS jsonb), :dedupKey, :readAt, :createdAt)
        ON CONFLICT (dedup_key) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id,
                       @Param("memberId") String memberId,
                       @Param("type") String type,
                       @Param("title") String title,
                       @Param("body") String body,
                       @Param("payload") String payload,
                       @Param("dedupKey") String dedupKey,
                       @Param("readAt") LocalDateTime readAt,
                       @Param("createdAt") LocalDateTime createdAt);

    Optional<Notification> findByIdAndMemberId(String id, String memberId);

    Page<Notification> findByMemberIdOrderByCreatedAtDescIdDesc(String memberId, Pageable pageable);

    Page<Notification> findByMemberIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(String memberId, Pageable pageable);

    long countByMemberIdAndReadAtIsNull(String memberId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.memberId = :memberId AND n.readAt IS NULL")
    int markAllRead(@Param("memberId") String memberId, @Param("now") LocalDateTime now);
}
