package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
}
