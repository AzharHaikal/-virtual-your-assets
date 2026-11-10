package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.entity.AdminActivity;
import com.vyra.be_virtual_your_assets.repository.AdminActivityRepository;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vyra.be_virtual_your_assets.entity.Admin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActivityServiceTest {

    @Mock
    private AdminActivityRepository adminActivityRepository;

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminActivityService adminActivityService;

    @BeforeEach
    void setUp() {
        when(adminActivityRepository.save(org.mockito.ArgumentMatchers.any(AdminActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private AdminActivity captureSaved() {
        ArgumentCaptor<AdminActivity> captor = ArgumentCaptor.forClass(AdminActivity.class);
        verify(adminActivityRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createAdminActivity_shouldPersistActivityWithCorrectFields() {
        ArgumentCaptor<AdminActivity> captor = ArgumentCaptor.forClass(AdminActivity.class);

        adminActivityService.createAdminActivity(
                "admin-id-001", "Budi Admin", AdminActivityEvent.SUCCESS_LOGIN, "127.0.0.1", "Chrome on Windows"
        );

        verify(adminActivityRepository).save(captor.capture());
        AdminActivity saved = captor.getValue();

        assertThat(saved.getAdminId()).isEqualTo("admin-id-001");
        assertThat(saved.getAdminName()).isEqualTo("Budi Admin");
        assertThat(saved.getEvent()).isEqualTo(AdminActivityEvent.SUCCESS_LOGIN.name());
        assertThat(saved.getDescription()).isEqualTo(AdminActivityEvent.SUCCESS_LOGIN.getDescription());
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getDevice()).isEqualTo("Chrome on Windows");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createAdminActivity_withNullIpAndDevice_shouldStillPersist() {
        ArgumentCaptor<AdminActivity> captor = ArgumentCaptor.forClass(AdminActivity.class);

        adminActivityService.createAdminActivity(
                "admin-id-002", "Sari Admin", AdminActivityEvent.SUCCESS_LOGOUT, null, null
        );

        verify(adminActivityRepository).save(captor.capture());
        AdminActivity saved = captor.getValue();

        assertThat(saved.getIpAddress()).isNull();
        assertThat(saved.getDevice()).isNull();
        assertThat(saved.getEvent()).isEqualTo(AdminActivityEvent.SUCCESS_LOGOUT.name());
    }


    // =========================================================================
    // request context + name resolution + defaults
    // =========================================================================
    @Test
    void createAdminActivity_withoutIpAndDevice_shouldUseRemoteAddrAndUserAgent_notAClientSetForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 Test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        adminActivityService.createAdminActivity("a-1", "Budi", AdminActivityEvent.UPDATE_ADMIN, null, null);

        AdminActivity saved = captureSaved();
        assertThat(saved.getIpAddress()).isEqualTo("198.51.100.10");
        assertThat(saved.getDevice()).isEqualTo("Mozilla/5.0 Test");
    }

    @Test
    void createAdminActivity_withoutForwardedFor_shouldUseRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.4");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        adminActivityService.createAdminActivity("a-1", "Budi", AdminActivityEvent.UPDATE_ADMIN, " ", "");

        AdminActivity saved = captureSaved();
        assertThat(saved.getIpAddress()).isEqualTo("198.51.100.4");
        assertThat(saved.getDevice()).isNull();
    }

    @Test
    void createAdminActivity_explicitIpAndDevice_shouldWinOverRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.4");
        request.addHeader("User-Agent", "Ignored");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        adminActivityService.createAdminActivity("a-1", "Budi", AdminActivityEvent.SUCCESS_LOGIN, "1.1.1.1", "Pixel");

        AdminActivity saved = captureSaved();
        assertThat(saved.getIpAddress()).isEqualTo("1.1.1.1");
        assertThat(saved.getDevice()).isEqualTo("Pixel");
    }

    @Test
    void createAdminActivity_nameEqualsId_shouldResolveRealAdminName() {
        Admin admin = new Admin();
        admin.setName("Budi Admin");
        when(adminRepository.findById("a-1")).thenReturn(Optional.of(admin));

        adminActivityService.createAdminActivity("a-1", "a-1", AdminActivityEvent.CREATE_ADMIN, null, null);

        assertThat(captureSaved().getAdminName()).isEqualTo("Budi Admin");
    }

    @Test
    void createAdminActivity_blankNameAndUnknownAdmin_shouldFallBackToId() {
        when(adminRepository.findById("a-9")).thenReturn(Optional.empty());

        adminActivityService.createAdminActivity("a-9", " ", AdminActivityEvent.CREATE_ADMIN, null, null);

        assertThat(captureSaved().getAdminName()).isEqualTo("a-9");
    }

    @Test
    void createAdminActivity_unknownEmailLogin_shouldStoreUnknownIdAndKeepName() {
        adminActivityService.createAdminActivity(null, "ghost@example.com", AdminActivityEvent.FAILED_LOGIN, null, null);

        AdminActivity saved = captureSaved();
        assertThat(saved.getAdminId()).isEqualTo("UNKNOWN");
        assertThat(saved.getAdminName()).isEqualTo("ghost@example.com");
    }

    @Test
    void createAdminActivity_noIdAndNoName_shouldStoreUnknownName() {
        adminActivityService.createAdminActivity(null, null, AdminActivityEvent.FAILED_LOGIN, null, null);

        assertThat(captureSaved().getAdminName()).isEqualTo("UNKNOWN");
    }
}
