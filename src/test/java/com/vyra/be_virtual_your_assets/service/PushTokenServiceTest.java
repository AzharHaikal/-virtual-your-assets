package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.DevicePlatform;
import com.vyra.be_virtual_your_assets.dto.push.RegisterPushTokenRequest;
import com.vyra.be_virtual_your_assets.entity.DevicePushToken;
import com.vyra.be_virtual_your_assets.repository.DevicePushTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 5, 0);

    @Mock private DevicePushTokenRepository repository;

    private PushTokenService service;

    @BeforeEach
    void setUp() {
        service = new PushTokenService(repository,
                new TimeService(Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void register_newToken_isStored_andAKnownOneMovesToTheSignedInMember() {
        service.register("m-1", request(" token-1 "));
        ArgumentCaptor<DevicePushToken> saved = ArgumentCaptor.forClass(DevicePushToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo("token-1");
        assertThat(saved.getValue().getMemberId()).isEqualTo("m-1");
        assertThat(saved.getValue().getLastSeenAt()).isEqualTo(NOW);

        DevicePushToken shared = new DevicePushToken();
        shared.setToken("token-2");
        shared.setMemberId("m-other");
        shared.setCreatedAt(NOW.minusDays(3));
        when(repository.findByToken("token-2")).thenReturn(Optional.of(shared));
        service.register("m-1", request("token-2"));
        assertThat(shared.getMemberId()).isEqualTo("m-1");
        assertThat(shared.getCreatedAt()).isEqualTo(NOW.minusDays(3));
    }

    @Test
    void unregister_removesOnlyTheOwnToken_andIgnoresBlank() {
        service.unregister("m-1", "token-1");
        verify(repository).deleteOwned("m-1", "token-1");

        service.unregister("m-1", " ");
        verify(repository, never()).deleteOwned("m-1", " ");
    }

    @Test
    void fcm_invalidTokenResponses_andTheMessageShape() {
        HttpClientErrorException gone = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null,
                "{\"error\":{\"status\":\"NOT_FOUND\"}}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        HttpClientErrorException unregistered = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null,
                "{\"error\":{\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        HttpClientErrorException badPayload = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null,
                "{\"error\":{\"message\":\"Invalid JSON payload\"}}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(FcmPushClient.isInvalidToken(gone)).isTrue();
        assertThat(FcmPushClient.isInvalidToken(unregistered)).isTrue();
        assertThat(FcmPushClient.isInvalidToken(badPayload)).isFalse();

        Map<String, Object> message = FcmPushClient.message("t", "Title", "Body", Map.of("route", "habit"));
        assertThat(message).containsEntry("token", "t").containsEntry("data", Map.of("route", "habit"));
        assertThat(message.get("notification")).isEqualTo(Map.of("title", "Title", "body", "Body"));
    }

    private static RegisterPushTokenRequest request(String token) {
        RegisterPushTokenRequest request = new RegisterPushTokenRequest();
        request.setToken(token);
        request.setPlatform(DevicePlatform.ANDROID);
        return request;
    }
}
