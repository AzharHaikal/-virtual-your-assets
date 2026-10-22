package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An e-mail address / phone number change that the member has requested (and confirmed with
 * the PIN) but not yet verified with the OTP. Applied to the member by
 * {@code ContactChangeService.verify}; at most one pending change per member.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "member_contact_change",
        uniqueConstraints = @UniqueConstraint(name = "uq_member_contact_change_member", columnNames = "member_id"))
public class MemberContactChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    /** Null when the e-mail address is not part of the change. */
    @Column(length = 50)
    private String newEmail;

    /** Null when the phone number is not part of the change. */
    @Column(length = 15)
    private String newPhoneNumber;

    private LocalDateTime createdAt;
}
