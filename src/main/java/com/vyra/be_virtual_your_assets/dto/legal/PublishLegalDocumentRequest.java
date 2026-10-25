package com.vyra.be_virtual_your_assets.dto.legal;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Publishes a new version; earlier versions stay in the history. */
@Data
public class PublishLegalDocumentRequest {
    @NotNull(message = "Type is required")
    private LegalDocumentType type;

    @NotNull(message = "Language is required")
    private AppLanguage language;

    @NotBlank(message = "Content is required")
    @Size(max = 100000, message = "Content is too long")
    private String content;
}
