package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.legal.LegalDocumentResponse;
import com.vyra.be_virtual_your_assets.dto.legal.PublishLegalDocumentRequest;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.LegalDocument;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.LegalDocumentRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Privacy policy and terms, per language. Every publish is a new version (the history is kept); the app
 * and the landing page read the latest one and fall back to their bundled text when none is published.
 */
@Service
@RequiredArgsConstructor
public class LegalDocumentService {

    private final LegalDocumentRepository documentRepository;
    private final AdminRepository adminRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;
    private final TimeService timeService;

    /** Public: the latest version, 404 when none is published yet. */
    @Transactional(readOnly = true)
    public BaseResponse<LegalDocumentResponse> getLatest(LegalDocumentType type, AppLanguage language) {
        LegalDocument document = documentRepository.findTopByTypeAndLanguageOrderByVersionDesc(type, language)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.LEGAL_DOCUMENT_NOT_FOUND));
        LegalDocumentResponse response = toResponse(document, true, null);
        response.setPublishedBy(null);
        return new BaseResponse<>(ErrorConstant.LEGAL_DOCUMENT_SUCCESS.getCode(),
                ErrorConstant.LEGAL_DOCUMENT_SUCCESS.getMessage(), response);
    }

    /** Portal: every version of a document, newest first (without the text). */
    @Transactional(readOnly = true)
    public BaseResponse<List<LegalDocumentResponse>> getVersions(LegalDocumentType type, AppLanguage language) {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_VIEW);
        List<LegalDocument> versions = documentRepository.findByTypeAndLanguageOrderByVersionDesc(type, language);
        Map<String, String> names = adminNames(versions);
        return new BaseResponse<>(ErrorConstant.LEGAL_DOCUMENT_SUCCESS.getCode(),
                ErrorConstant.LEGAL_DOCUMENT_SUCCESS.getMessage(),
                versions.stream().map(d -> toResponse(d, false, names.get(d.getPublishedBy()))).toList());
    }

    @Transactional(readOnly = true)
    public BaseResponse<LegalDocumentResponse> getVersion(String documentId) {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_VIEW);
        LegalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.LEGAL_DOCUMENT_NOT_FOUND));
        return new BaseResponse<>(ErrorConstant.LEGAL_DOCUMENT_SUCCESS.getCode(),
                ErrorConstant.LEGAL_DOCUMENT_SUCCESS.getMessage(),
                toResponse(document, true, adminNames(List.of(document)).get(document.getPublishedBy())));
    }

    @Transactional
    public BaseResponse<LegalDocumentResponse> publish(PublishLegalDocumentRequest request, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_MANAGE);
        int version = documentRepository.findTopByTypeAndLanguageOrderByVersionDesc(request.getType(), request.getLanguage())
                .map(LegalDocument::getVersion)
                .orElse(0) + 1;
        LegalDocument document = new LegalDocument();
        document.setType(request.getType());
        document.setLanguage(request.getLanguage());
        document.setVersion(version);
        document.setContent(request.getContent().strip());
        document.setPublishedAt(timeService.nowUtc());
        document.setPublishedBy(admin.getAdminId());
        documentRepository.saveAndFlush(document);
        adminActivityService.createAdminActivity(admin.getAdminId(), null, AdminActivityEvent.PUBLISH_LEGAL_DOCUMENT,
                request.getType() + " " + request.getLanguage() + " v" + version, null, null);
        return new BaseResponse<>(ErrorConstant.LEGAL_DOCUMENT_PUBLISHED.getCode(),
                ErrorConstant.LEGAL_DOCUMENT_PUBLISHED.getMessage(), toResponse(document, true, null));
    }

    private Map<String, String> adminNames(List<LegalDocument> documents) {
        return adminRepository.findAllById(documents.stream().map(LegalDocument::getPublishedBy).distinct().toList())
                .stream().collect(Collectors.toMap(Admin::getId, Admin::getName, (a, b) -> a));
    }

    private static LegalDocumentResponse toResponse(LegalDocument document, boolean withContent, String adminName) {
        LegalDocumentResponse response = new LegalDocumentResponse();
        response.setId(document.getId());
        response.setType(document.getType());
        response.setLanguage(document.getLanguage());
        response.setVersion(document.getVersion());
        response.setContent(withContent ? document.getContent() : null);
        response.setPublishedAt(document.getPublishedAt());
        response.setPublishedBy(adminName);
        return response;
    }
}
