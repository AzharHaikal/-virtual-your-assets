package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.GoalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GoalMilestoneRepository extends JpaRepository<GoalMilestone, String> {

    Optional<GoalMilestone> findByIdAndGoalId(String id, String goalId);

    /** Milestones of the goals in their order. */
    @Query("SELECT m FROM GoalMilestone m WHERE m.goalId IN :goalIds ORDER BY m.position, m.createdAt")
    List<GoalMilestone> findAllOfGoals(@Param("goalIds") Collection<String> goalIds);

    long countByGoalId(String goalId);

    @Query("SELECT COALESCE(MAX(m.position), 0) FROM GoalMilestone m WHERE m.goalId = :goalId")
    int maxPosition(@Param("goalId") String goalId);
}
