package com.vyra.virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(schema = "idp", name = "member_activity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberActivityId;

    @Column(length = 50)
    private String phoneNumber;

    @Column(length = 100)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
