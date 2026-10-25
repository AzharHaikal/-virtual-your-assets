package com.vyra.be_virtual_your_assets.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Claude Messages API. Off without {@code vyra.ai.api-key} (ANTHROPIC_API_KEY). Server-side refusal fallback
 * is on, so a declined request is retried on a fallback model chosen by the API. Prompt and answer are never
 * logged.
 */
@Component
@Slf4j
public class ClaudeReflectionClient implements AiReflectionClient {

    static final long MAX_TOKENS = 16000L;
    static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final AnthropicClient client;
    private final String model;

    public ClaudeReflectionClient(@Value("${vyra.ai.api-key:}") String apiKey,
                                  @Value("${vyra.ai.model:claude-opus-5-5}") String model,
                                  @Value("${vyra.ai.timeout-seconds:60}") long timeoutSeconds) {
        this.model = model;
        this.client = StringUtils.isBlank(apiKey) ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(2)
                .build();
        log.info("AI reflection {}", client == null ? "disabled (no API key)" : "enabled. model: " + model);
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    public Optional<Result> generate(String system, String prompt) {
        if (client == null) {
            return Optional.empty();
        }
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(system)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(prompt);
        if (supportsFallback(model)) {
            builder.putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        MessageCreateParams params = builder.build();
        try {
            Message response = client.messages().create(params);
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                log.warn("AI reflection declined by the model");
                return Optional.empty();
            }
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining("\n"))
                    .strip();
            return text.isEmpty() ? Optional.empty() : Optional.of(new Result(text, response.model().asString()));
        } catch (RateLimitException e) {
            log.warn("AI reflection rate limited");
        } catch (AnthropicServiceException e) {
            log.warn("AI reflection failed. status: {}", e.statusCode());
        } catch (AnthropicIoException e) {
            log.warn("AI reflection unreachable. reason: {}", e.getClass().getSimpleName());
        } catch (AnthropicException e) {
            log.warn("AI reflection error. reason: {}", e.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    /** Server-side refusal fallback exists for the Opus, Sonnet and Fable models, not for Haiku. */
    static boolean supportsFallback(String model) {
        return model.startsWith("claude-opus-") || model.startsWith("claude-sonnet-") || model.startsWith("claude-fable-");
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }
}
