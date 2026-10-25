package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One published version of a legal document in one language; never changed afterwards. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "admin_idp", name = "legal_document")
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 10)
    private LegalDocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private AppLanguage language;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private String publishedBy;
}
