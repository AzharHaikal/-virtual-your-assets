package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.push.RegisterPushTokenRequest;
import com.vyra.be_virtual_your_assets.entity.DevicePushToken;
import com.vyra.be_virtual_your_assets.repository.DevicePushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** The devices that receive the member's push notifications. Tokens are never logged. */
@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final DevicePushTokenRepository tokenRepository;
    private final TimeService timeService;

    /** Idempotent; a token registered by another member before (shared device) moves to this member. */
    @Transactional
    public BaseResponse<Void> register(String memberId, RegisterPushTokenRequest request) {
        LocalDateTime now = timeService.nowUtc();
        String token = request.getToken().trim();
        DevicePushToken device = tokenRepository.findByToken(token).orElseGet(() -> {
            DevicePushToken created = new DevicePushToken();
            created.setToken(token);
            created.setCreatedAt(now);
            return created;
        });
        device.setMemberId(memberId);
        device.setPlatform(request.getPlatform());
        device.setLastSeenAt(now);
        tokenRepository.save(device);
        return new BaseResponse<>(ErrorConstant.PUSH_TOKEN_SAVED.getCode(), ErrorConstant.PUSH_TOKEN_SAVED.getMessage(), null);
    }

    /** Idempotent; only the member's own token is removed. */
    @Transactional
    public BaseResponse<Void> unregister(String memberId, String token) {
        if (StringUtils.isNotBlank(token)) {
            tokenRepository.deleteOwned(memberId, token.trim());
        }
        return new BaseResponse<>(ErrorConstant.PUSH_TOKEN_DELETED.getCode(), ErrorConstant.PUSH_TOKEN_DELETED.getMessage(), null);
    }
}
