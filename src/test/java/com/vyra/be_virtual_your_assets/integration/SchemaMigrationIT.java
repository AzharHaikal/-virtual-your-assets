package com.vyra.be_virtual_your_assets.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the Flyway migrations alone build a schema the entities accept.
 * <p>
 * The application runs with {@code ddl-auto=validate}, so the Spring context only starts when
 * every entity matches a table/column created by {@code db/migration}. Run it against an
 * <b>empty</b> database (GitLab CI job {@code migration-check} does this):
 *
 * <pre>
 *   DATABASE_URL=jdbc:postgresql://localhost:5432/empty_db DATABASE_USERNAME=... DATABASE_PASSWORD=...
 *   MAIL_USERNAME=x MAIL_PASSWORD=x CRYPTO_SECRET_KEY=0123456789abcdef0123456789abcdef
 *   mvn test -Dtest=SchemaMigrationIT -Dvyra.it.db=true
 * </pre>
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class SchemaMigrationIT {

    @Autowired private Flyway flyway;

    @Test
    void migrationsApplyAndEntitiesValidate() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(Arrays.stream(applied).allMatch(m -> m.getState().isApplied() && !m.getState().isFailed()))
                .as("every migration applied without failure")
                .isTrue();
        assertThat(flyway.info().pending()).as("no pending migrations").isEmpty();
    }
}
