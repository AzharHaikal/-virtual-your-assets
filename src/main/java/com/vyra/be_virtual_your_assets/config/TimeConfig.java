package com.vyra.be_virtual_your_assets.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Time zone setup (see {@code TimeService}): one UTC clock, and every {@link LocalDateTime} in a JSON
 * response written as a UTC instant with a trailing {@code Z} so clients convert it to local time.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcLocalDateTimeSerializer() {
        return builder -> builder.serializerByType(LocalDateTime.class, new UtcLocalDateTimeSerializer());
    }

    /** {@code 2026-10-22T01:30:00Z}: the value is a UTC instant (the application runs in UTC). */
    static class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }
}
