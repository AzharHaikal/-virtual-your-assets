package com.vyra.be_virtual_your_assets.service.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeReflectionClientTest {

    @Test
    void withoutApiKey_isUnavailableAndNeverCallsTheApi() {
        ClaudeReflectionClient client = new ClaudeReflectionClient("", "claude-opus-5-5", 60);

        assertThat(client.isAvailable()).isFalse();
        assertThat(client.generate("system", "prompt")).isEmpty();
    }

    @Test
    void refusalFallback_onlyForModelsThatSupportIt() {
        assertThat(ClaudeReflectionClient.supportsFallback("claude-opus-5-5")).isTrue();
        assertThat(ClaudeReflectionClient.supportsFallback("claude-sonnet-5-5")).isTrue();
        assertThat(ClaudeReflectionClient.supportsFallback("claude-haiku-5-5")).isFalse();
    }

    @Test
    void withApiKey_isAvailable() {
        ClaudeReflectionClient client = new ClaudeReflectionClient("sk-ant-test", "claude-opus-5-5", 60);
        try {
            assertThat(client.isAvailable()).isTrue();
        } finally {
            client.close();
        }
    }
}
