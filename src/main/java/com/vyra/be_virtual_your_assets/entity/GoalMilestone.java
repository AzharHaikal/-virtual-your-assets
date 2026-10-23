package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A step of a goal; done when completedAt is set. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "goal_milestone")
public class GoalMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String goalId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private int position;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public boolean isCompleted() {
        return completedAt != null;
    }
}
