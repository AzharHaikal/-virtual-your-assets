package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.constant.MemberStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.vyra.virtual_your_assets.constant.MemberActivityEvent;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "wallet", name = "member_wallet")
@Entity
public class MemberWallet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

}
