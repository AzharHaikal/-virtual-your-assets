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

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${vyra.admin.seed.name:Super Administrator}")
    private String seedName;

    @Value("${vyra.admin.seed.email:superadmin@vyra.local}")
    private String seedEmail;

    @Value("${vyra.admin.seed.phone-number:628000000000}")
    private String seedPhoneNumber;

    @Value("${vyra.admin.seed.password:ChangeMe123!}")
    private String seedPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) {
            log.info("Admin table already has data, skipping default admin seed.");
            return;
        }

        Admin admin = new Admin();
        admin.setName(seedName);
        admin.setEmail(seedEmail.toLowerCase().trim());
        admin.setPhoneNumber(seedPhoneNumber);
        admin.setPassword(passwordEncoder.encode(seedPassword));
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
