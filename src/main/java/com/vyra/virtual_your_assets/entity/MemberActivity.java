package com.vyra.virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_activity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberActivityId;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
