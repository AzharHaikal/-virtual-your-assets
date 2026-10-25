package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import com.vyra.be_virtual_your_assets.dto.legal.LegalDocumentResponse;
import com.vyra.be_virtual_your_assets.dto.legal.PublishLegalDocumentRequest;
import com.vyra.be_virtual_your_assets.entity.LegalDocument;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.LegalDocumentRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceTest {

    @Mock private LegalDocumentRepository documentRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;

    private LegalDocumentService service;
    private final CustomAdminDetails admin = new CustomAdminDetails("admin-1", "token", AdminRole.ADMINISTRATOR);

    @BeforeEach
    void setUp() {
        service = new LegalDocumentService(documentRepository, adminRepository, adminAuthorizationService,
                adminActivityService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void getLatest_isPublicAndHidesThePublisher() {
        when(documentRepository.findTopByTypeAndLanguageOrderByVersionDesc(LegalDocumentType.PRIVACY, AppLanguage.ID))
                .thenReturn(Optional.of(document(3)));

        LegalDocumentResponse result = service.getLatest(LegalDocumentType.PRIVACY, AppLanguage.ID).getData();

        assertThat(result.getVersion()).isEqualTo(3);
        assertThat(result.getContent()).isEqualTo("Isi v3");
        assertThat(result.getPublishedBy()).isNull();
        verifyNoInteractions(adminAuthorizationService);
    }

    @Test
    void getLatest_nothingPublished_isNotFound() {
        when(documentRepository.findTopByTypeAndLanguageOrderByVersionDesc(LegalDocumentType.TERMS, AppLanguage.EN))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getLatest(LegalDocumentType.TERMS, AppLanguage.EN));
    }

    @Test
    void publish_addsTheNextVersionAndAudits() {
        when(documentRepository.findTopByTypeAndLanguageOrderByVersionDesc(LegalDocumentType.PRIVACY, AppLanguage.ID))
                .thenReturn(Optional.of(document(3)));
        PublishLegalDocumentRequest request = new PublishLegalDocumentRequest();
        request.setType(LegalDocumentType.PRIVACY);
        request.setLanguage(AppLanguage.ID);
        request.setContent("  Kebijakan baru  ");

        LegalDocumentResponse result = service.publish(request, admin).getData();

        verify(adminAuthorizationService).requirePermission(AdminPermission.SETTINGS_MANAGE);
        ArgumentCaptor<LegalDocument> saved = ArgumentCaptor.forClass(LegalDocument.class);
        verify(documentRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getVersion()).isEqualTo(4);
        assertThat(saved.getValue().getContent()).isEqualTo("Kebijakan baru");
        assertThat(saved.getValue().getPublishedBy()).isEqualTo("admin-1");
        assertThat(result.getVersion()).isEqualTo(4);
        verify(adminActivityService).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.PUBLISH_LEGAL_DOCUMENT), contains("v4"), isNull(), isNull());
    }

    @Test
    void publish_firstVersion_isOne() {
        when(documentRepository.findTopByTypeAndLanguageOrderByVersionDesc(LegalDocumentType.TERMS, AppLanguage.EN))
                .thenReturn(Optional.empty());
        PublishLegalDocumentRequest request = new PublishLegalDocumentRequest();
        request.setType(LegalDocumentType.TERMS);
        request.setLanguage(AppLanguage.EN);
        request.setContent("Terms");

        assertThat(service.publish(request, admin).getData().getVersion()).isEqualTo(1);
    }

    private static LegalDocument document(int version) {
        LegalDocument document = new LegalDocument();
        document.setId("doc-" + version);
        document.setType(LegalDocumentType.PRIVACY);
        document.setLanguage(AppLanguage.ID);
        document.setVersion(version);
        document.setContent("Isi v" + version);
        document.setPublishedBy("admin-1");
        return document;
    }
}
