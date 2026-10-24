package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Goal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, String> {

    Optional<Goal> findByIdAndMemberIdAndDeletedAtIsNull(String id, String memberId);

    /** Locks the goal so parallel changes add up instead of overwriting each other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Goal g WHERE g.id = :id AND g.memberId = :memberId AND g.deletedAt IS NULL")
    Optional<Goal> findOwnedForUpdate(@Param("id") String id, @Param("memberId") String memberId);

    /** The member's goals, not deleted: nearest due date first (none last), then the oldest. */
    @Query("""
        SELECT g FROM Goal g
        WHERE g.memberId = :memberId AND g.deletedAt IS NULL
        ORDER BY g.dueDate ASC NULLS LAST, g.createdAt, g.id
        """)
    List<Goal> findAllOfMember(@Param("memberId") String memberId);

    long countByMemberIdAndDeletedAtIsNull(String memberId);

    /** Active savings targets of the member. Row: [count, sum of targets, sum of current values]. */
    @Query("""
        SELECT COUNT(g), COALESCE(SUM(g.targetValue), 0), COALESCE(SUM(g.currentValue), 0)
        FROM Goal g
        WHERE g.memberId = :memberId AND g.deletedAt IS NULL AND g.linkedToFinance = true
          AND g.status = com.vyra.be_virtual_your_assets.constant.GoalStatus.ACTIVE
        """)
    List<Object[]> sumSavingsTargets(@Param("memberId") String memberId);
}
