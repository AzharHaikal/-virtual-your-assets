package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MoodTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MoodTagRepository extends JpaRepository<MoodTag, String> {

    /** Built-in tags and the member's own, not deleted: built-in first. */
    @Query("""
        SELECT t FROM MoodTag t
        WHERE (t.memberId IS NULL OR t.memberId = :memberId)
          AND t.deletedAt IS NULL
        ORDER BY CASE WHEN t.memberId IS NULL THEN 0 ELSE 1 END, t.position, LOWER(t.name)
        """)
    List<MoodTag> findVisible(@Param("memberId") String memberId);

    Optional<MoodTag> findByIdAndDeletedAtIsNull(String id);

    long countByMemberIdAndDeletedAtIsNull(String memberId);

    @Query("""
        SELECT COUNT(t) > 0 FROM MoodTag t
        WHERE (t.memberId IS NULL OR t.memberId = :memberId)
          AND LOWER(t.name) = LOWER(:name)
          AND t.deletedAt IS NULL
        """)
    boolean existsVisibleName(@Param("memberId") String memberId, @Param("name") String name);
}
