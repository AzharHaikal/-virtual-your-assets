package com.vyra.be_virtual_your_assets.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.security.filter.MaintenanceFilter;
import com.vyra.be_virtual_your_assets.service.SystemSettings;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceFilterTest {

    @Mock private SystemSettings systemSettings;
    @Mock private FilterChain chain;

    private MaintenanceFilter filter;

    @BeforeEach
    void setUp() {
        filter = new MaintenanceFilter(systemSettings, new ObjectMapper());
    }

    @Test
    void maintenanceOn_memberApi_is503WithThePortalMessage() throws Exception {
        when(systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED)).thenReturn(true);
        when(systemSettings.text(SystemConfigKey.MAINTENANCE_MESSAGE)).thenReturn("Back at 10:00");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/habits"), response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains(ErrorConstant.MAINTENANCE_MODE.getCode(), "Back at 10:00");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void maintenanceOn_blankMessage_usesTheDefaultText() throws Exception {
        when(systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED)).thenReturn(true);
        when(systemSettings.text(SystemConfigKey.MAINTENANCE_MESSAGE)).thenReturn("");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/v1/auth/login"), response, chain);

        assertThat(response.getContentAsString()).contains(ErrorConstant.MAINTENANCE_MODE.getMessage());
    }

    @Test
    void maintenanceOff_passesThrough() throws Exception {
        when(systemSettings.isEnabled(SystemConfigKey.MAINTENANCE_ENABLED)).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/habits");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void adminStatusLegalAndActuator_alwaysOpen() throws Exception {
        for (String path : new String[]{"/api/v1/admin/settings", "/api/v1/status", "/api/v1/legal/PRIVACY",
                "/actuator/health", "/swagger-ui/index.html"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
        }
        verifyNoInteractions(systemSettings);
    }
}
