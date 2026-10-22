package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.entity.IdempotencyKey;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * Runs a request at most once per (member, Idempotency-Key). The first request reserves the key and
 * stores its response; a retry with the same key and body gets that stored response back instead of
 * running again (e.g. the app resends a create transaction after a timeout).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    static final int MAX_KEY_LENGTH = 100;
    static final long TTL_HOURS = 24;

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    private final TimeService timeService;

    /**
     * @param key      Idempotency-Key header; blank runs {@code action} without protection (older apps)
     * @param request  request body, hashed to detect a key reused for a different request
     * @param dataType type of {@code BaseResponse.data}, to read a stored response back
     */
    public <T> BaseResponse<T> execute(String memberId, String key, Object request, Class<T> dataType,
                                       Supplier<BaseResponse<T>> action) {
        if (StringUtils.isBlank(key)) {
            return action.get();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException(ErrorConstant.IDEMPOTENCY_KEY_INVALID);
        }

        String id = memberId + ":" + key;
        String requestHash = sha256(toJson(request));
        idempotencyKeyRepository.deleteExpired(memberId, timeService.nowUtc().minusHours(TTL_HOURS));

        if (idempotencyKeyRepository.reserve(id, memberId, requestHash, timeService.nowUtc()) == 0) {
            return replay(id, requestHash, dataType);
        }

        BaseResponse<T> response;
        try {
            response = action.get();
        } catch (RuntimeException e) {
            // Nothing was done: free the key so the client can retry it.
            idempotencyKeyRepository.deleteById(id);
            throw e;
        }
        idempotencyKeyRepository.complete(id, toJson(response));
        return response;
    }

    private <T> BaseResponse<T> replay(String id, String requestHash, Class<T> dataType) {
        IdempotencyKey existing = idempotencyKeyRepository.findById(id)
                .orElseThrow(() -> new ConflictException(ErrorConstant.IDEMPOTENCY_KEY_IN_PROGRESS));
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ConflictException(ErrorConstant.IDEMPOTENCY_KEY_REUSED);
        }
        if (existing.getResponseBody() == null) {
            throw new ConflictException(ErrorConstant.IDEMPOTENCY_KEY_IN_PROGRESS);
        }
        log.info("Idempotent replay for key {}", id.substring(id.indexOf(':') + 1));
        JavaType type = objectMapper.getTypeFactory().constructParametricType(BaseResponse.class, dataType);
        try {
            return objectMapper.readValue(existing.getResponseBody(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored idempotent response cannot be read", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize for idempotency", e);
        }
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
