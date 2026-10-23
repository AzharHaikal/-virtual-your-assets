package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.account.AccountResponse;
import com.vyra.be_virtual_your_assets.dto.account.DeleteAccountOtpResponse;
import com.vyra.be_virtual_your_assets.dto.account.DeleteAccountRequest;
import com.vyra.be_virtual_your_assets.dto.account.UpdatePreferencesRequest;
import com.vyra.be_virtual_your_assets.dto.member.VerifyContactChangeRequest;
import com.vyra.be_virtual_your_assets.entity.MemberPhoto;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.AccountDeletionService;
import com.vyra.be_virtual_your_assets.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/** The signed-in member's own account (REST style; the /member endpoints stay for older apps). */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_USERS_ME)
public class AccountController {

    private final AccountService accountService;
    private final AccountDeletionService accountDeletionService;

    @GetMapping
    public BaseResponse<AccountResponse> getAccount(Authentication authentication) {
        return accountService.getAccount(memberId(authentication));
    }

    @PatchMapping(ApiPath.USERS_ME_PREFERENCES)
    public BaseResponse<AccountResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request,
            Authentication authentication
    ) {
        return accountService.updatePreferences(memberId(authentication), request);
    }

    /**
     * Raw image bytes as the body (Content-Type image/jpeg, image/png or image/webp). At most one byte
     * more than the limit is read, so a huge upload never fills the memory.
     */
    @PutMapping(ApiPath.USERS_ME_PHOTO)
    public BaseResponse<AccountResponse> savePhoto(HttpServletRequest request, Authentication authentication)
            throws IOException {
        byte[] content;
        try (InputStream body = request.getInputStream()) {
            content = body.readNBytes(AccountService.MAX_PHOTO_BYTES + 1);
        }
        return accountService.savePhoto(memberId(authentication), content);
    }

    @GetMapping(ApiPath.USERS_ME_PHOTO)
    public ResponseEntity<byte[]> getPhoto(Authentication authentication) {
        MemberPhoto photo = accountService.getPhoto(memberId(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(photo.getContent());
    }

    @DeleteMapping(ApiPath.USERS_ME_PHOTO)
    public BaseResponse<AccountResponse> deletePhoto(Authentication authentication) {
        return accountService.deletePhoto(memberId(authentication));
    }

    /** Delete the own account, step 1: PIN; an OTP is e-mailed. */
    @PostMapping(ApiPath.USERS_ME_DELETE_REQUEST)
    public BaseResponse<DeleteAccountOtpResponse> requestDeletion(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication
    ) {
        return accountDeletionService.request(memberId(authentication), request.getPin());
    }

    @PostMapping(ApiPath.USERS_ME_DELETE_RESEND)
    public BaseResponse<DeleteAccountOtpResponse> resendDeletionOtp(Authentication authentication) {
        return accountDeletionService.resend(memberId(authentication));
    }

    /** Step 2: the OTP; the account becomes DELETED and every session ends. */
    @PostMapping(ApiPath.USERS_ME_DELETE)
    public BaseResponse<Void> confirmDeletion(
            @Valid @RequestBody VerifyContactChangeRequest request,
            Authentication authentication
    ) {
        return accountDeletionService.confirm(memberId(authentication), request.getOtpCode());
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
