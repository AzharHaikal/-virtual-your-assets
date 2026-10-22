package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One Idempotency-Key of a member. Written only through {@code IdempotencyKeyRepository}'s atomic
 * reserve / complete queries; {@link #responseBody} is null while the first request is still running.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "transaction_finance", name = "idempotency_key")
public class IdempotencyKey {

    @Id
    @Column(length = 200)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(columnDefinition = "text")
    private String responseBody;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
