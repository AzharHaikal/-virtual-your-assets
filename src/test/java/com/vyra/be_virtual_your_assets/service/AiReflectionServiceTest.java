package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ConsentType;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.insight.AiReflectionResponse;
import com.vyra.be_virtual_your_assets.dto.insight.AiReflectionStatusResponse;
import com.vyra.be_virtual_your_assets.dto.insight.WeeklyInsightResponse;
import com.vyra.be_virtual_your_assets.entity.AiReflection;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.exception.ServiceUnavailableException;
import com.vyra.be_virtual_your_assets.repository.AiReflectionRepository;
import com.vyra.be_virtual_your_assets.service.ai.AiReflectionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiReflectionServiceTest {

    /** Today is Sunday 2026-10-25 (WIB): the last full week is 12..18 October. */
    private static final LocalDate LAST_MONDAY = LocalDate.of(2026, 10, 12);

    @Mock private AiReflectionRepository reflectionRepository;
    @Mock private AiReflectionClient aiClient;
    @Mock private ConsentService consentService;
    @Mock private InsightService insightService;
    @Mock private ValidationService validationService;

    private AiReflectionService service;
    private final Member member = new Member();

    @BeforeEach
    void setUp() {
        service = new AiReflectionService(reflectionRepository, aiClient, consentService, insightService,
                validationService, new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
        member.setId("m-1");
        member.setTimezone("Asia/Jakarta");
        member.setLanguage(AppLanguage.ID);
    }

    @Test
    void create_withoutApiKey_isUnavailableBeforeAnythingElse() {
        when(aiClient.isAvailable()).thenReturn(false);

        ServiceUnavailableException e = assertThrows(ServiceUnavailableException.class, () -> service.create("m-1"));

        assertThat(e.getErrorConstant()).isEqualTo(ErrorConstant.AI_REFLECTION_UNAVAILABLE);
        verifyNoInteractions(consentService, insightService);
    }

    @Test
    void create_withoutConsent_isForbidden() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(consentService.isGranted("m-1", ConsentType.AI_REFLECTION)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.create("m-1"));
        verify(aiClient, never()).generate(anyString(), anyString());
    }

    @Test
    void create_alreadyThisWeek_isConflict() {
        allowed();
        when(reflectionRepository.existsByMemberIdAndPeriodStartAndDeletedAtIsNull("m-1", LAST_MONDAY)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create("m-1"));
    }

    @Test
    void create_emptyWeek_isNoData() {
        allowed();
        when(insightService.getWeekly("m-1", LAST_MONDAY)).thenReturn(week(0, 0, 0, null, 0, "0", null));

        assertThrows(BadRequestException.class, () -> service.create("m-1"));
        verify(aiClient, never()).generate(anyString(), anyString());
    }

    @Test
    void create_sendsOnlyAggregatesAndStoresTheText() {
        allowed();
        when(insightService.getWeekly("m-1", LAST_MONDAY))
                .thenReturn(week(10, 7, 70, "3.50", 5, "450000", "-12.5"));
        when(insightService.getWeekly("m-1", LAST_MONDAY.minusDays(7)))
                .thenReturn(week(10, 5, 50, "3.00", 4, "500000", null));
        when(aiClient.generate(contains("Indonesian"), anyString()))
                .thenReturn(Optional.of(new AiReflectionClient.Result("Minggu yang stabil.", "claude-opus-5-5")));

        AiReflectionResponse result = service.create("m-1").getData();

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiClient).generate(anyString(), prompt.capture());
        assertThat(prompt.getValue())
                .contains("7 of 10 (70%)", "week before: 50%", "average mood 3.50 of 5", "-12.5%")
                .doesNotContain("450000", "500000");
        ArgumentCaptor<AiReflection> saved = ArgumentCaptor.forClass(AiReflection.class);
        verify(reflectionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPeriodStart()).isEqualTo(LAST_MONDAY);
        assertThat(saved.getValue().getPeriodEnd()).isEqualTo(LAST_MONDAY.plusDays(6));
        assertThat(saved.getValue().getModel()).isEqualTo("claude-opus-5-5");
        assertThat(result.getContent()).isEqualTo("Minggu yang stabil.");
    }

    @Test
    void create_modelFails_isUnavailableAndNothingSaved() {
        allowed();
        when(insightService.getWeekly("m-1", LAST_MONDAY)).thenReturn(week(3, 1, 33, null, 0, "0", null));
        when(insightService.getWeekly("m-1", LAST_MONDAY.minusDays(7))).thenReturn(week(0, 0, 0, null, 0, "0", null));
        when(aiClient.generate(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceUnavailableException.class, () -> service.create("m-1"));
        verify(reflectionRepository, never()).saveAndFlush(any());
    }

    @Test
    void getStatus_combinesAvailabilityConsentAndData() {
        when(validationService.getMemberById("m-1")).thenReturn(member);
        when(aiClient.isAvailable()).thenReturn(true);
        when(consentService.isGranted("m-1", ConsentType.AI_REFLECTION)).thenReturn(true);
        when(insightService.getWeekly("m-1", LAST_MONDAY)).thenReturn(week(0, 0, 0, "4.00", 2, "0", null));

        AiReflectionStatusResponse status = service.getStatus("m-1").getData();

        assertThat(status.getPeriodStart()).isEqualTo(LAST_MONDAY);
        assertThat(status.isHasData()).isTrue();
        assertThat(status.isCanCreate()).isTrue();
    }

    @Test
    void delete_otherMembersReflection_isNotFound() {
        when(reflectionRepository.findByIdAndMemberIdAndDeletedAtIsNull("x-1", "m-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("m-1", "x-1"));
    }

    @Test
    void systemPrompt_followsTheMembersLanguageAndForbidsDiagnosis() {
        assertThat(AiReflectionService.systemPrompt(AppLanguage.EN)).contains("Write in English", "do not diagnose");
        assertThat(AiReflectionService.systemPrompt(null)).contains("Indonesian");
    }

    private void allowed() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(consentService.isGranted("m-1", ConsentType.AI_REFLECTION)).thenReturn(true);
        when(validationService.getMemberById("m-1")).thenReturn(member);
    }

    private static BaseResponse<WeeklyInsightResponse> week(int scheduled, int done, Integer percent, String moodAverage,
                                                            int moodEntries, String expense, String changePercent) {
        WeeklyInsightResponse week = new WeeklyInsightResponse();
        WeeklyInsightResponse.Habits habits = new WeeklyInsightResponse.Habits();
        habits.setScheduled(scheduled);
        habits.setDone(done);
        habits.setPercent(scheduled == 0 ? null : percent);
        week.setHabits(habits);
        WeeklyInsightResponse.Mood mood = new WeeklyInsightResponse.Mood();
        mood.setEntries(moodEntries);
        mood.setAverage(moodAverage == null ? null : new BigDecimal(moodAverage));
        week.setMood(mood);
        WeeklyInsightResponse.Expense spending = new WeeklyInsightResponse.Expense();
        spending.setThisWeek(new BigDecimal(expense));
        spending.setLastWeek(BigDecimal.ZERO);
        spending.setChangePercent(changePercent == null ? null : new BigDecimal(changePercent));
        week.setExpense(spending);
        return new BaseResponse<>("VYRA-INS-WKL-000", "ok", week);
    }
}
