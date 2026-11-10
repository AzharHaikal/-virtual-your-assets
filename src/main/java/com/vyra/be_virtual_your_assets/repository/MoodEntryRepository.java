package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MoodEntryRepository extends JpaRepository<MoodEntry, String> {

    Optional<MoodEntry> findByMemberIdAndLocalDateAndDeletedAtIsNull(String memberId, LocalDate localDate);

    /** The member's entries in [from, to], newest day first, tags loaded. */
    @Query("""
        SELECT DISTINCT e FROM MoodEntry e
        LEFT JOIN FETCH e.tags
        WHERE e.memberId = :memberId
          AND e.deletedAt IS NULL
          AND e.localDate >= :from
          AND e.localDate <= :to
        ORDER BY e.localDate DESC
        """)
    List<MoodEntry> findRange(@Param("memberId") String memberId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    // ─── Portal Mood report (SUPER_ADMIN) ────────────────────────────────────────

    /** Check-ins of every member in [from, to] with their member ([MoodEntry, Member]), newest first. */
    @Query(value = """
        SELECT e, m FROM MoodEntry e
        JOIN Member m ON m.id = e.memberId
        WHERE e.deletedAt IS NULL
          AND e.localDate >= :from
          AND e.localDate <= :to
          AND m.deletedAt IS NULL
          AND (
            LOWER(m.email) LIKE :pattern
            OR m.phoneNumber LIKE :pattern
            OR LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE :pattern
          )
        ORDER BY e.localDate DESC, e.checkedInAt DESC, e.id
        """, countQuery = """
        SELECT COUNT(e) FROM MoodEntry e
        JOIN Member m ON m.id = e.memberId
        WHERE e.deletedAt IS NULL
          AND e.localDate >= :from
          AND e.localDate <= :to
          AND m.deletedAt IS NULL
          AND (
            LOWER(m.email) LIKE :pattern
            OR m.phoneNumber LIKE :pattern
            OR LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE :pattern
          )
        """)
    Page<Object[]> findForReport(@Param("from") LocalDate from,
                                 @Param("to") LocalDate to,
                                 @Param("pattern") String pattern,
                                 Pageable pageable);

    /** [entryId, tag name] of the entries, in tag order. */
    @Query("""
        SELECT e.id, t.name FROM MoodEntry e
        JOIN e.tags t
        WHERE e.id IN :entryIds
        ORDER BY t.position, t.name
        """)
    List<Object[]> findTagNames(@Param("entryIds") Collection<String> entryIds);
}
