package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.crypto.TextEncryptConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A weekly AI reflection, the member's own text: encrypted at rest, never logged, deletable. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "ai_reflection")
public class AiReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    /** Monday of the reflected week (member's days). */
    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Convert(converter = TextEncryptConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
