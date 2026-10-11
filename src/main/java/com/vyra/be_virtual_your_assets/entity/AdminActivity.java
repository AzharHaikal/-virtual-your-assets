package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "admin_idp", name = "admin_activity",
        indexes = {
                @Index(name = "idx_admin_activity_created_at", columnList = "created_at")
        })
@Entity
public class AdminActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String adminId;

    @Column(nullable = false, length = 50)
    private String adminName;

    private String event;
    private String description;

    private String ipAddress;
    private String device;

    private LocalDateTime createdAt;
}
