package com.vyra.be_virtual_your_assets.seeder;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock private AdminRepository adminRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private ApplicationArguments applicationArguments;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminSeeder, "seedName", "Super Administrator");
        ReflectionTestUtils.setField(adminSeeder, "seedEmail", "superadmin@vyra.local");
        ReflectionTestUtils.setField(adminSeeder, "seedPhoneNumber", "628000000000");
        ReflectionTestUtils.setField(adminSeeder, "seedPassword", "ChangeMe123!");
    }

    @Test
    void run_adminTableEmpty_shouldCreateDefaultSuperAdmin() {
        when(adminRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("ChangeMe123!")).thenReturn("hashed-default-password");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminSeeder.run(applicationArguments);

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());

        Admin saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("superadmin@vyra.local");
        assertThat(saved.getPassword()).isEqualTo("hashed-default-password");
        assertThat(saved.getRole()).isEqualTo(AdminRole.SUPER_ADMIN);
        assertThat(saved.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void run_adminTableAlreadyHasData_shouldSkipSeedingAndNeverSave() {
        when(adminRepository.count()).thenReturn(5L);

        adminSeeder.run(applicationArguments);

        verify(adminRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void run_emailWithMixedCaseAndWhitespace_shouldNormalizeToLowerTrimmed() {
        ReflectionTestUtils.setField(adminSeeder, "seedEmail", "  SuperAdmin@VYRA.local  ");
        when(adminRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminSeeder.run(applicationArguments);

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("superadmin@vyra.local");
    }
}
