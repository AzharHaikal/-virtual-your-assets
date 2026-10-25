package com.vyra.be_virtual_your_assets.dto.legal;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LegalDocumentResponse {
    private String id;
    private LegalDocumentType type;
    private AppLanguage language;
    private int version;
    /** Plain text with line breaks; null in version lists. */
    private String content;
    private LocalDateTime publishedAt;
    /** Admin name; only for the portal. */
    private String publishedBy;
}
