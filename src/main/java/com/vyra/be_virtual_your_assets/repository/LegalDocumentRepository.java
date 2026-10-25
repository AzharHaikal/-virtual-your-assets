package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import com.vyra.be_virtual_your_assets.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, String> {

    Optional<LegalDocument> findTopByTypeAndLanguageOrderByVersionDesc(LegalDocumentType type, AppLanguage language);

    /** Every version, newest first. */
    List<LegalDocument> findByTypeAndLanguageOrderByVersionDesc(LegalDocumentType type, AppLanguage language);
}
