package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One fixed rate-limit window per key (bucket + client address). Shared by every backend
 * instance through the database; rows are written only by {@code DatabaseRateLimiter}
 * with an atomic upsert and purged once they are a day old.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "rate_limit_bucket")
public class RateLimitBucket {

    @Id
    @Column(length = 120)
    private String bucketKey;

    /** Start of the current window, epoch milliseconds (database clock). */
    @Column(nullable = false)
    private long windowStartMillis;

    @Column(nullable = false)
    private int hits;
}
