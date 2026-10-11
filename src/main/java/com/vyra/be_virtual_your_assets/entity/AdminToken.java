package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "admin_idp", name = "admin_token",
        indexes = {
                @Index(name = "idx_admin_token_admin_id", columnList = "admin_id")
        })
@Entity
public class AdminToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String adminTokenId;

    @Column(nullable = false)
    private String adminId;

    @Column(nullable = false, unique = true)
    private String accessToken;

    @Column(nullable = false, unique = true)
    private String refreshToken;

    private String deviceId;
    private String deviceName;
    private String ipAddress;

    private LocalDateTime accessTokenExpiredAt;
    private LocalDateTime refreshTokenExpiredAt;
    private LocalDateTime createdAt;
}
