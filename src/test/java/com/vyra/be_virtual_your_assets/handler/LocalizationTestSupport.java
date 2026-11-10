package com.vyra.be_virtual_your_assets.handler;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/** The MessageSource Spring Boot builds from application.yaml (spring.messages.*). */
final class LocalizationTestSupport {

    private LocalizationTestSupport() {
    }

    static MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
