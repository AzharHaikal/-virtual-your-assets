package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.push.RegisterPushTokenRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.PushTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Devices of the signed-in member that receive push notifications. */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_USERS_ME)
public class PushTokenController {

    private final PushTokenService pushTokenService;

    /** Call after login and whenever FCM gives the app a new token. */
    @PutMapping(ApiPath.USERS_ME_PUSH_TOKENS)
    public BaseResponse<Void> register(@Valid @RequestBody RegisterPushTokenRequest request, Authentication authentication) {
        return pushTokenService.register(memberId(authentication), request);
    }

    @DeleteMapping(ApiPath.USERS_ME_PUSH_TOKEN)
    public BaseResponse<Void> unregister(@PathVariable String token, Authentication authentication) {
        return pushTokenService.unregister(memberId(authentication), token);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
