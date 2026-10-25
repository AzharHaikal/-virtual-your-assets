package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.legal.LegalDocumentResponse;
import com.vyra.be_virtual_your_assets.dto.status.AppStatusResponse;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.security.filter.MaintenanceFilter;
import com.vyra.be_virtual_your_assets.service.LegalDocumentService;
import com.vyra.be_virtual_your_assets.service.SystemSettings;
import com.vyra.be_virtual_your_assets.service.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints without sign-in: the app's maintenance check and the legal documents (also for the landing page). */
@RestController
@RequiredArgsConstructor
public class PublicController {

    private final SystemSettings systemSettings;
    private final LegalDocumentService legalDocumentService;
    private final TimeService timeService;

    /** Works during maintenance too; the app checks it at start and after a 503. */
    @GetMapping(ApiPath.V1_STATUS)
    public BaseResponse<AppStatusResponse> getStatus() {
        boolean maintenance = systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED);
        return new BaseResponse<>(ErrorConstant.SYSTEM_STATUS_SUCCESS.getCode(),
                ErrorConstant.SYSTEM_STATUS_SUCCESS.getMessage(),
                new AppStatusResponse(maintenance, maintenance ? MaintenanceFilter.message(systemSettings) : null,
                        timeService.nowUtc()));
    }

    /** type = privacy | terms; language = ID | EN (default ID). */
    @GetMapping(ApiPath.V1_LEGAL)
    public BaseResponse<LegalDocumentResponse> getLegalDocument(
            @PathVariable String type,
            @RequestParam(defaultValue = "ID") AppLanguage language
    ) {
        LegalDocumentType documentType = LegalDocumentType.parse(type)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.LEGAL_DOCUMENT_NOT_FOUND));
        return legalDocumentService.getLatest(documentType, language);
    }
}
