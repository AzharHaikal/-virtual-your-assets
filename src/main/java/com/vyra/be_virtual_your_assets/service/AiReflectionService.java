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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Weekly AI reflection (Phase 3.5), only with the AI_REFLECTION consent. The model gets the week's
 * aggregates and nothing else: no identity, journal, habit or goal names, categories or amounts.
 * One reflection per week (the last full Monday..Sunday); the member can delete it and create it again.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiReflectionService {

    static final String SYSTEM_PROMPT = """
            You write a short weekly reflection for a personal wellbeing app. You get only aggregated numbers \
            for one week of one person: habit completion, mood check-ins (scale 1 to 5) and the change in \
            spending against the week before. Write in %s, warm and plain, 80 to 150 words, as plain text \
            without headings, lists or markdown. Describe what the numbers show, mention one thing that went \
            well and suggest one small, practical step for next week. You are not a doctor, therapist or \
            financial adviser: do not diagnose, do not give medical, psychological or financial advice, and do \
            not guess causes the numbers cannot show. If the average mood is low, gently say that talking to \
            someone they trust or to a professional can help.""";

    private final AiReflectionRepository reflectionRepository;
    private final AiReflectionClient aiClient;
    private final ConsentService consentService;
    private final InsightService insightService;
    private final ValidationService validationService;
    private final TimeService timeService;

    public BaseResponse<AiReflectionStatusResponse> getStatus(String memberId) {
        Member member = validationService.getMemberById(memberId);
        LocalDate monday = lastFullWeek(member);
        AiReflectionStatusResponse status = new AiReflectionStatusResponse();
        status.setAvailable(aiClient.isAvailable());
        status.setConsentGiven(consentService.isGranted(memberId, ConsentType.AI_REFLECTION));
        status.setPeriodStart(monday);
        status.setPeriodEnd(monday.plusDays(6));
        status.setAlreadyCreated(reflectionRepository.existsByMemberIdAndPeriodStartAndDeletedAtIsNull(memberId, monday));
        status.setHasData(hasData(insightService.getWeekly(memberId, monday).getData()));
        status.setCanCreate(status.isAvailable() && status.isConsentGiven() && status.isHasData() && !status.isAlreadyCreated());
        return new BaseResponse<>(ErrorConstant.AI_REFLECTION_STATUS_SUCCESS.getCode(),
                ErrorConstant.AI_REFLECTION_STATUS_SUCCESS.getMessage(), status);
    }

    @Transactional(readOnly = true)
    public BaseResponse<List<AiReflectionResponse>> getAll(String memberId) {
        return new BaseResponse<>(ErrorConstant.AI_REFLECTION_LIST_SUCCESS.getCode(),
                ErrorConstant.AI_REFLECTION_LIST_SUCCESS.getMessage(),
                reflectionRepository.findTop20ByMemberIdAndDeletedAtIsNullOrderByPeriodStartDesc(memberId).stream()
                        .map(AiReflectionService::toResponse).toList());
    }

    /** Not transactional: the model call can take seconds and must not hold a database connection. */
    public BaseResponse<AiReflectionResponse> create(String memberId) {
        if (!aiClient.isAvailable()) {
            throw new ServiceUnavailableException(ErrorConstant.AI_REFLECTION_UNAVAILABLE);
        }
        if (!consentService.isGranted(memberId, ConsentType.AI_REFLECTION)) {
            throw new ForbiddenException(ErrorConstant.AI_REFLECTION_CONSENT_REQUIRED);
        }
        Member member = validationService.getMemberById(memberId);
        LocalDate monday = lastFullWeek(member);
        if (reflectionRepository.existsByMemberIdAndPeriodStartAndDeletedAtIsNull(memberId, monday)) {
            throw new ConflictException(ErrorConstant.AI_REFLECTION_EXISTS);
        }
        WeeklyInsightResponse week = insightService.getWeekly(memberId, monday).getData();
        if (!hasData(week)) {
            throw new BadRequestException(ErrorConstant.AI_REFLECTION_NO_DATA);
        }
        WeeklyInsightResponse previous = insightService.getWeekly(memberId, monday.minusDays(7)).getData();

        AiReflectionClient.Result result = aiClient.generate(systemPrompt(member.getLanguage()), prompt(week, previous))
                .orElseThrow(() -> new ServiceUnavailableException(ErrorConstant.AI_REFLECTION_UNAVAILABLE));

        AiReflection reflection = new AiReflection();
        reflection.setMemberId(memberId);
        reflection.setPeriodStart(monday);
        reflection.setPeriodEnd(monday.plusDays(6));
        reflection.setContent(result.text());
        reflection.setModel(result.model());
        reflection.setCreatedAt(timeService.nowUtc());
        try {
            reflectionRepository.saveAndFlush(reflection);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ErrorConstant.AI_REFLECTION_EXISTS);
        }
        log.info("AI reflection created. reflectionId: {} week: {}", reflection.getId(), monday);
        return new BaseResponse<>(ErrorConstant.AI_REFLECTION_CREATED.getCode(),
                ErrorConstant.AI_REFLECTION_CREATED.getMessage(), toResponse(reflection));
    }

    /** Removed for good: the text is the member's and they asked for it to go. */
    @Transactional
    public BaseResponse<Void> delete(String memberId, String reflectionId) {
        AiReflection reflection = reflectionRepository.findByIdAndMemberIdAndDeletedAtIsNull(reflectionId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.AI_REFLECTION_NOT_FOUND));
        reflectionRepository.delete(reflection);
        log.info("AI reflection deleted. reflectionId: {}", reflectionId);
        return new BaseResponse<>(ErrorConstant.AI_REFLECTION_DELETED.getCode(),
                ErrorConstant.AI_REFLECTION_DELETED.getMessage(), null);
    }

    private LocalDate lastFullWeek(Member member) {
        return timeService.today(timeService.zoneOf(member))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(7);
    }

    static boolean hasData(WeeklyInsightResponse week) {
        return week.getHabits().getScheduled() > 0 || week.getMood().getEntries() > 0
                || week.getExpense().getThisWeek().signum() > 0;
    }

    static String systemPrompt(AppLanguage language) {
        return SYSTEM_PROMPT.formatted(language == AppLanguage.EN ? "English" : "Indonesian (Bahasa Indonesia)");
    }

    /** The only data that leaves the server: counts, averages and a percentage. */
    static String prompt(WeeklyInsightResponse week, WeeklyInsightResponse previous) {
        StringBuilder text = new StringBuilder("Week summary:\n");
        WeeklyInsightResponse.Habits habits = week.getHabits();
        text.append("- Scheduled habits done: ").append(habits.getDone()).append(" of ").append(habits.getScheduled());
        if (habits.getPercent() != null) {
            text.append(" (").append(habits.getPercent()).append("%)");
        }
        if (previous.getHabits().getPercent() != null) {
            text.append("; week before: ").append(previous.getHabits().getPercent()).append("%");
        }
        text.append('\n');
        text.append("- Mood check-ins: ").append(week.getMood().getEntries()).append(" of 7 days");
        if (week.getMood().getAverage() != null) {
            text.append(", average mood ").append(week.getMood().getAverage()).append(" of 5");
        }
        if (previous.getMood().getAverage() != null) {
            text.append("; week before: ").append(previous.getMood().getAverage());
        }
        text.append('\n');
        if (week.getExpense().getChangePercent() != null) {
            text.append("- Spending against the week before: ")
                    .append(week.getExpense().getChangePercent().signum() > 0 ? "+" : "")
                    .append(week.getExpense().getChangePercent()).append("%\n");
        } else {
            text.append("- Spending against the week before: no comparison available\n");
        }
        return text.toString();
    }

    private static AiReflectionResponse toResponse(AiReflection reflection) {
        return new AiReflectionResponse(reflection.getId(), reflection.getPeriodStart(), reflection.getPeriodEnd(),
                reflection.getContent(), reflection.getCreatedAt());
    }
}
