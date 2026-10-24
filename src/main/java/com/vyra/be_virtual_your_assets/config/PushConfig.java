package com.vyra.be_virtual_your_assets.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.vyra.be_virtual_your_assets.service.FcmPushClient;
import com.vyra.be_virtual_your_assets.service.PushClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Push notifications through FCM when a Firebase service account is configured
 * ({@code vyra.push.fcm.credentials}: the JSON itself or a path to it); otherwise push is off and
 * notifications stay in-app only.
 */
@Configuration
@Slf4j
public class PushConfig {

    @Bean
    public PushClient pushClient(@Value("${vyra.push.fcm.credentials:}") String credentials,
                                 @Value("${vyra.push.fcm.project-id:}") String projectId) throws IOException {
        if (StringUtils.isBlank(credentials)) {
            log.info("Push notifications are off (no Firebase credentials)");
            return disabled();
        }
        GoogleCredentials google;
        try (InputStream in = open(credentials.trim())) {
            google = GoogleCredentials.fromStream(in).createScoped(FcmPushClient.SCOPE);
        }
        String project = StringUtils.isNotBlank(projectId) ? projectId
                : google instanceof ServiceAccountCredentials account ? account.getProjectId() : null;
        if (StringUtils.isBlank(project)) {
            throw new IllegalStateException("vyra.push.fcm.project-id is required for these credentials");
        }
        log.info("Push notifications through FCM are on");
        return new FcmPushClient(google, project, RestClient.create());
    }

    private static InputStream open(String credentials) throws IOException {
        return credentials.startsWith("{")
                ? new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))
                : new FileInputStream(credentials);
    }

    static PushClient disabled() {
        return new PushClient() {
            @Override
            public Result send(String token, String title, String body, Map<String, String> data) {
                return Result.FAILED;
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        };
    }
}
