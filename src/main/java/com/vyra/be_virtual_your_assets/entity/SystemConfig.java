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
 * One portal-managed setting. Keys are defined by {@code SystemConfigKey}; values are stored as text.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "admin_idp", name = "system_config")
public class SystemConfig {

    @Id
    @Column(length = 100)
    private String configKey;

    @Column(nullable = false, length = 500)
    private String configValue;

    /** Admin id of the last change; null for the seeded default. */
    private String updatedBy;

    private LocalDateTime updatedAt;
}
