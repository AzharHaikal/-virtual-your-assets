package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "idp", name = "member_token")
@Entity
public class MemberToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberTokenId;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false, unique = true)
    private String accessToken;

    @Column(nullable = false, unique = true)
    private String refreshToken;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String deviceName;

    @Column(nullable = false)
    private String ipAddress;

    private LocalDateTime accessTokenExpiredAt;
    private LocalDateTime refreshTokenExpiredAt;
    private LocalDateTime createdAt;
}
