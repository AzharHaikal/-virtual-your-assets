package com.vyra.be_virtual_your_assets.seeder;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Bootstraps a default SUPER_ADMIN account on first startup so the portal
 * always has at least one way in. Only runs when the admin table is empty -
 * it never overwrites or duplicates existing admin accounts.
 *
 * Default credentials are configurable via application.yaml / environment
 * variables (vyra.admin.seed.*) and should be rotated immediately after
 * the first login in any non-local environment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    /** Built-in fallback of vyra.admin.seed.password; only acceptable for local development. */
    static final String DEFAULT_PASSWORD = "ChangeMe123!";
    static final int MIN_DEPLOYED_PASSWORD_LENGTH = 12;
    private static final Profiles DEPLOYED = Profiles.of("sit", "uat", "prod");

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${vyra.admin.seed.name:Super Administrator}")
    private String seedName;

    @Value("${vyra.admin.seed.email:superadmin@vyra.local}")
    private String seedEmail;

    @Value("${vyra.admin.seed.phone-number:628000000000}")
    private String seedPhoneNumber;

    @Value("${vyra.admin.seed.password:" + DEFAULT_PASSWORD + "}")
    private String seedPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) {
            log.info("Admin table already has data, skipping default admin seed.");
            return;
        }

        // An empty VYRA_ADMIN_SEED_PASSWORD (e.g. copied from .env.example) counts as not set.
        String password = seedPassword == null || seedPassword.isBlank() ? DEFAULT_PASSWORD : seedPassword;
        if (environment.acceptsProfiles(DEPLOYED)
                && (DEFAULT_PASSWORD.equals(password) || password.length() < MIN_DEPLOYED_PASSWORD_LENGTH)) {
            // Fail the first start instead of shipping a SUPER_ADMIN with a password that is in the source code.
            throw new IllegalStateException("Set VYRA_ADMIN_SEED_PASSWORD to a strong password (at least "
                    + MIN_DEPLOYED_PASSWORD_LENGTH + " characters, not the built-in default) before the first start"
                    + " of a sit/uat/prod environment: it becomes the password of the first SUPER_ADMIN.");
        }

        Admin admin = new Admin();
        admin.setName(seedName);
        admin.setEmail(seedEmail.toLowerCase().trim());
        admin.setPhoneNumber(seedPhoneNumber);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setStatus(ActiveStatus.ACTIVE);
        admin.setCreatedBy("SYSTEM");
        admin.setCreatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        log.warn("======================================================================");
        log.warn(" Default SUPER_ADMIN account created.");
        log.warn(" Email: {}", seedEmail);
        log.warn(" Password: (see vyra.admin.seed.password / VYRA_ADMIN_SEED_PASSWORD)");
        log.warn(" Please log in and change this password immediately.");
        log.warn("======================================================================");
    }
}
