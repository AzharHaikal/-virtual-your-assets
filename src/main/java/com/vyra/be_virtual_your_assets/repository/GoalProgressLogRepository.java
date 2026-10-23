package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalProgressLogRepository extends JpaRepository<GoalProgressLog, String> {

    /** The latest 20 entries, newest first. */
    List<GoalProgressLog> findTop20ByGoalIdOrderByCreatedAtDesc(String goalId);
}
