package com.vyra.be_virtual_your_assets.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * application-{sit,uat,prod}.yaml are loaded on top of application.yaml by SPRING_PROFILES_ACTIVE.
 * Only the config files are read: an empty context, no database or beans.
 */
class ProfileConfigTest {

    static class Empty {
    }

    private static ConfigurableApplicationContext start(String... profiles) {
        return new SpringApplicationBuilder(Empty.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .logStartupInfo(false)
                .run();
    }

    @ParameterizedTest
    @CsvSource({
            "sit, jdbc:postgresql://localhost:5432/sit_vyra_assets",
            "uat, jdbc:postgresql://localhost:5432/uat_vyra_assets",
            "prod, jdbc:postgresql://localhost:5432/prod_vyra_assets"
    })
    void profile_shouldUseItsOwnDatabase(String profile, String url) {
        assumeTrue(System.getenv("DATABASE_URL") == null, "DATABASE_URL overrides the profile default");
        try (ConfigurableApplicationContext context = start(profile)) {
            Environment env = context.getEnvironment();
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo(url);
            // Everything else still comes from application.yaml.
            assertThat(env.getProperty("spring.flyway.default-schema")).isEqualTo("flyway");
        }
    }

    @Test
    void noProfile_shouldKeepLocalDevelopmentDatabase() {
        assumeTrue(System.getenv("DATABASE_URL") == null, "DATABASE_URL overrides the default");
        try (ConfigurableApplicationContext context = start()) {
            assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://localhost:5432/vyra_assets");
        }
    }

    @Test
    void prod_shouldKeepApiDocsOff() {
        try (ConfigurableApplicationContext context = start("prod")) {
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        }
    }

    @Test
    void sit_shouldOpenApiDocsByDefault() {
        assumeTrue(System.getenv("VYRA_API_DOCS_ENABLED") == null, "VYRA_API_DOCS_ENABLED overrides the default");
        try (ConfigurableApplicationContext context = start("sit")) {
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true");
        }
    }
}
