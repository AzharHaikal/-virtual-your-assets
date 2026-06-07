package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "idp", name = "member_activity")
@Entity
public class MemberActivity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 15)
    private String phoneNumber;

    private String event;
    private String description;
    private LocalDateTime createdAt;
}
