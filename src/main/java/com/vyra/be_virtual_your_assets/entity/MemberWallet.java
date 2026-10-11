package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "wallet", name = "member_wallet",
        indexes = {
                @Index(name = "idx_member_wallet_phone", columnList = "phone_number"),
                @Index(name = "idx_member_wallet_member_id", columnList = "member_id")
        })
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
    private ActiveStatus status;

}
