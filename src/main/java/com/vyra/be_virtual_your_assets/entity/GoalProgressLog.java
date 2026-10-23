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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A change of a goal's value (delta and the value after it) and/or a note. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "goal_progress_log")
public class GoalProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String goalId;

    @Column(precision = 19, scale = 2)
    private BigDecimal delta;

    @Column(precision = 19, scale = 2)
    private BigDecimal valueAfter;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
