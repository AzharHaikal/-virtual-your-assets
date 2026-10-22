package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.service.TimeService;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    /** Consecutive wrong PINs (login, change PIN, contact change). Null is treated as 0. */
    private Integer failedPinAttempts;

    /** While in the future every PIN-based action is refused with HTTP 429. */
    private LocalDateTime pinLockedUntil;

    /** While in the future OTP verification and OTP e-mails are refused with HTTP 429. */
    private LocalDateTime otpLockedUntil;

    /** IANA time zone (e.g. Asia/Jakarta): decides the member's "today" and the local date of their data. */
    @Column(nullable = false, length = 64)
    private String timezone;

    @PrePersist
    void defaultTimezone() {
        if (timezone == null) {
            timezone = TimeService.FALLBACK_ZONE_ID;
        }
    }
}
