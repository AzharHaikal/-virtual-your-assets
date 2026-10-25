package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Cover image of a goal (already compressed by the app); one row per goal. Never removed: removing the
 * image or deleting the goal marks it DELETED (soft delete, like the goal itself).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "goal_image")
public class GoalImage {

    @Id
    private String goalId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** ACTIVE or DELETED only (goal_image_status_check). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    private LocalDateTime deletedAt;

    public void markDeleted(LocalDateTime at) {
        status = ActiveStatus.DELETED;
        deletedAt = at;
    }
}
