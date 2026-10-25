package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.ConsentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One consent of a member. Revoking keeps the row with {@code revokedAt}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(MemberConsent.Key.class)
@Table(schema = "idp", name = "member_consent")
public class MemberConsent {

    @Id
    private String memberId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConsentType consentType;

    private LocalDateTime grantedAt;

    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isGranted() {
        return grantedAt != null && revokedAt == null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String memberId;
        private ConsentType consentType;
    }
}
