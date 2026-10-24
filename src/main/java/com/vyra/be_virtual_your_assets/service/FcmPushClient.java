package com.vyra.be_virtual_your_assets.service;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Firebase Cloud Messaging HTTP v1 API with the service account's OAuth token. */
@Slf4j
public class FcmPushClient implements PushClient {

    public static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String SEND_URL = "https://fcm.googleapis.com/v1/projects/{projectId}/messages:send";

    private final GoogleCredentials credentials;
    private final String projectId;
    private final RestClient restClient;

    public FcmPushClient(GoogleCredentials credentials, String projectId, RestClient restClient) {
        this.credentials = credentials;
        this.projectId = projectId;
        this.restClient = restClient;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Result send(String token, String title, String body, Map<String, String> data) {
        try {
            credentials.refreshIfExpired();
            restClient.post()
                    .uri(SEND_URL, projectId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials.getAccessToken().getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message(token, title, body, data)))
                    .retrieve()
                    .toBodilessEntity();
            return Result.SENT;
        } catch (HttpClientErrorException e) {
            if (isInvalidToken(e)) {
                return Result.INVALID_TOKEN;
            }
            log.warn("FCM rejected a push. status: {}", e.getStatusCode().value());
            return Result.FAILED;
        } catch (IOException | RestClientException e) {
            log.warn("FCM push failed. reason: {}", e.getClass().getSimpleName());
            return Result.FAILED;
        }
    }

    /** 404 UNREGISTERED, or 400 about the registration token. */
    static boolean isInvalidToken(HttpClientErrorException e) {
        if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            return true;
        }
        String response = e.getResponseBodyAsString();
        return e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()
                && (response.contains("UNREGISTERED") || response.contains("registration token"));
    }

    static Map<String, Object> message(String token, String title, String body, Map<String, String> data) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", token);
        message.put("notification", Map.of("title", title, "body", body));
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }
        message.put("android", Map.of("priority", "HIGH"));
        return message;
    }
}
