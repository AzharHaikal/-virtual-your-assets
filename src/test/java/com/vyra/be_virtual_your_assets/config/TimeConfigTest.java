package com.vyra.be_virtual_your_assets.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTest {

    private final TimeConfig timeConfig = new TimeConfig();

    @Test
    void localDateTime_isWrittenAsUtcInstant() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        timeConfig.utcLocalDateTimeSerializer().customize(builder);
        ObjectMapper mapper = builder.build();

        String json = mapper.writeValueAsString(Map.of("at", LocalDateTime.of(2026, 10, 22, 17, 30, 5)));

        assertThat(json).isEqualTo("{\"at\":\"2026-10-22T17:30:05Z\"}");
    }

    @Test
    void clock_isUtc() {
        Clock clock = timeConfig.clock();
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
