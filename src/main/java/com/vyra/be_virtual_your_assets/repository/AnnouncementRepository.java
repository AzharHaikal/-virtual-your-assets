package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, String> {

    Optional<Announcement> findByIdAndDeletedAtIsNull(String id);

    /** Portal list: newest first. */
    Page<Announcement> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    /** Published and within their period at {@code now}, for the given language (or all), newest first. */
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.deletedAt IS NULL
          AND a.status = com.vyra.be_virtual_your_assets.constant.AnnouncementStatus.PUBLISHED
          AND a.startsAt <= :now AND (a.endsAt IS NULL OR a.endsAt > :now)
          AND (a.language = 'ALL' OR a.language = :language)
        ORDER BY a.startsAt DESC
        """)
    List<Announcement> findActive(@Param("now") LocalDateTime now, @Param("language") String language);

    /** Active and not yet sent as notifications. */
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.deletedAt IS NULL AND a.deliveredAt IS NULL
          AND a.status = com.vyra.be_virtual_your_assets.constant.AnnouncementStatus.PUBLISHED
          AND a.startsAt <= :now AND (a.endsAt IS NULL OR a.endsAt > :now)
        """)
    List<Announcement> findToDeliver(@Param("now") LocalDateTime now);
}
