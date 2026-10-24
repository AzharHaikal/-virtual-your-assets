package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GoalProgressLogRepository extends JpaRepository<GoalProgressLog, String> {

    /** The latest 20 entries, newest first. */
    List<GoalProgressLog> findTop20ByGoalIdOrderByCreatedAtDesc(String goalId);

    /** Every entry of the goals, oldest first (data export). */
    List<GoalProgressLog> findByGoalIdInOrderByCreatedAtAsc(Collection<String> goalIds);

    /** Rows: [GoalProgressLog, Goal]; the latest 5 of the member's goals that are not deleted. */
    @Query("""
        SELECT l, g FROM GoalProgressLog l, Goal g
        WHERE g.id = l.goalId AND g.memberId = :memberId AND g.deletedAt IS NULL
        ORDER BY l.createdAt DESC
        LIMIT 5
        """)
    List<Object[]> findLatestOfMember(@Param("memberId") String memberId);
}
