package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.consent.ConsentRequest;
import com.vyra.be_virtual_your_assets.dto.consent.ConsentResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.ConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The member's explicit opt-ins, e.g. AI_REFLECTION. */
@RestController
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @GetMapping(ApiPath.V1_USERS_ME + ApiPath.USERS_ME_CONSENTS)
    public BaseResponse<List<ConsentResponse>> getAll(Authentication authentication) {
        return consentService.getAll(memberId(authentication));
    }

    @PutMapping(ApiPath.V1_USERS_ME + ApiPath.USERS_ME_CONSENT)
    public BaseResponse<ConsentResponse> set(
            @PathVariable String type,
            @Valid @RequestBody ConsentRequest request,
            Authentication authentication
    ) {
        return consentService.set(memberId(authentication), type, request.getGranted());
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
