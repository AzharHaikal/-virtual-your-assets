package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Progress of a habit on one of the member's days; written with an upsert (one row per habit and day). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "habit_completion")
public class HabitCompletion {

    @Id
    private String id;

    @Column(nullable = false)
    private String habitId;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private LocalDate localDate;

    @Column(nullable = false)
    private int count;

    @Column(nullable = false)
    private LocalDateTime completedAt;
}
