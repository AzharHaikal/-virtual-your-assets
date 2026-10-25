package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.entity.GoalImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoalImageRepository extends JpaRepository<GoalImage, String> {

    /** The image a member can see; a DELETED one is kept but never shown or exported. */
    default Optional<GoalImage> findActive(String goalId) {
        return findByGoalIdAndStatus(goalId, ActiveStatus.ACTIVE);
    }

    Optional<GoalImage> findByGoalIdAndStatus(String goalId, ActiveStatus status);
}
