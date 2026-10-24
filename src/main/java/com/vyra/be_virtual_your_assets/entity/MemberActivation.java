package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
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

/** When a member first did a core action (activation metrics, read by the admin metrics of Phase 3). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(MemberActivation.Key.class)
@Table(schema = "idp", name = "member_activation")
public class MemberActivation {

    @Id
    private String memberId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ActivationEvent event;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String memberId;
        private ActivationEvent event;
    }
}
