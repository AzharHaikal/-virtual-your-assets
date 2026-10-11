package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "idp", name = "member",
        indexes = {
                @Index(name = "idx_member_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_member_created_at", columnList = "created_at")
        })
@Entity
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(nullable = false)
    private String pin;

    @Enumerated(EnumType.STRING)
    private ActiveStatus status;
}
