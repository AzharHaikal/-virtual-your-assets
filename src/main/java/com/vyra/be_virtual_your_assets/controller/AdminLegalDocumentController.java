package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.legal.LegalDocumentResponse;
import com.vyra.be_virtual_your_assets.dto.legal.PublishLegalDocumentRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.LegalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Legal documents builder of the portal (SETTINGS_VIEW to read, SETTINGS_MANAGE to publish). */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_LEGAL)
public class AdminLegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping
    public BaseResponse<List<LegalDocumentResponse>> getVersions(
            @RequestParam LegalDocumentType type,
            @RequestParam AppLanguage language
    ) {
        return legalDocumentService.getVersions(type, language);
    }

    @GetMapping(ApiPath.ADMIN_LEGAL_ID)
    public BaseResponse<LegalDocumentResponse> getVersion(@PathVariable String documentId) {
        return legalDocumentService.getVersion(documentId);
    }

    @PostMapping
    public ResponseEntity<BaseResponse<LegalDocumentResponse>> publish(
            @Valid @RequestBody PublishLegalDocumentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(legalDocumentService.publish(request, (CustomAdminDetails) authentication.getPrincipal()));
    }
}
