package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.export.DataExportRequest;
import com.vyra.be_virtual_your_assets.dto.export.DataExportResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.DataExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Export of all the member's personal data (Profil > Unduh data saya). */
@RestController
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService dataExportService;

    /** Needs the PIN; the export is built in the background (202) and announced by e-mail. Once per day. */
    @PostMapping(ApiPath.V1_USERS_ME + ApiPath.USERS_ME_EXPORT)
    public ResponseEntity<BaseResponse<DataExportResponse>> request(
            @Valid @RequestBody DataExportRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dataExportService.request(memberId(authentication), request.getPin()));
    }

    /** The latest export (data null when none). */
    @GetMapping(ApiPath.V1_USERS_ME + ApiPath.USERS_ME_EXPORT)
    public BaseResponse<DataExportResponse> getLatest(Authentication authentication) {
        return dataExportService.getLatest(memberId(authentication));
    }

    @GetMapping(ApiPath.V1_USERS_ME + ApiPath.USERS_ME_EXPORT_ID)
    public BaseResponse<DataExportResponse> get(@PathVariable String exportId, Authentication authentication) {
        return dataExportService.get(memberId(authentication), exportId);
    }

    /** The ZIP, from the app. */
    @GetMapping(ApiPath.V1_USERS_ME + ApiPath.USERS_ME_EXPORT_DOWNLOAD)
    public ResponseEntity<byte[]> download(@PathVariable String exportId, Authentication authentication) {
        return MemberTransactionController.download(dataExportService.download(memberId(authentication), exportId));
    }

    /** The ZIP, from the e-mailed link: no session, the token authorises it. */
    @GetMapping(ApiPath.V1_EXPORT_PUBLIC_DOWNLOAD)
    public ResponseEntity<byte[]> downloadWithToken(@PathVariable String exportId, @RequestParam String token) {
        return MemberTransactionController.download(dataExportService.downloadWithToken(exportId, token));
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
