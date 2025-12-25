package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.constant.OtpType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member_otp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberOtpId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String otpCode;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    private OtpType otpType;

    @Column
    private Integer attempts;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
