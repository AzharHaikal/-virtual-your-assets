package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.insight.WeeklyInsightResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.InsightService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightControllerTest {

    @Mock private InsightService insightService;

    @InjectMocks
    private InsightController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void weekly_delegatesWithTheAuthenticatedMember() {
        LocalDate date = LocalDate.of(2026, 10, 21);
        BaseResponse<WeeklyInsightResponse> weekly = new BaseResponse<>("VYRA-INS-WKL-000", "ok", null);
        when(insightService.getWeekly("member-1", date)).thenReturn(weekly);

        assertThat(controller.getWeekly(date, authentication)).isSameAs(weekly);
    }
}
