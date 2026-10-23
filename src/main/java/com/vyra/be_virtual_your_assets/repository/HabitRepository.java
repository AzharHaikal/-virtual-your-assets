package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, String> {

    Optional<Habit> findByIdAndMemberIdAndDeletedAtIsNull(String id, String memberId);

    /** The member's habits, not deleted, oldest first (the order they were added). */
    @Query("""
        SELECT h FROM Habit h
        WHERE h.memberId = :memberId AND h.deletedAt IS NULL
        ORDER BY h.createdAt, h.id
        """)
    List<Habit> findAllOfMember(@Param("memberId") String memberId);

    long countByMemberIdAndDeletedAtIsNull(String memberId);
}
