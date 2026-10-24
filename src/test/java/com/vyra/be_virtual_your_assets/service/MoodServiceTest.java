package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.TrendPeriod;
import com.vyra.be_virtual_your_assets.dto.mood.CreateMoodTagRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTagCountResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTagsSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTrendResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.entity.MoodTag;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.MoodTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-22 17:30 UTC = 2026-10-23 00:30 in Jakarta. */
    private static final LocalDate TODAY_JAKARTA = LocalDate.of(2026, 10, 23);

    @Mock private MoodEntryRepository entryRepository;
    @Mock private MoodTagRepository tagRepository;
    @Mock private ValidationService validationService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ActivationService activationService;

    private MoodService moodService;
    private Member member;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        moodService = new MoodService(entryRepository, tagRepository, validationService, timeService, transactionManager,
                activationService);
        member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(entryRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // saveEntry
    // =========================================================================
    @Test
    void saveEntry_newDay_createsEntryWithTagsAndTrimmedNote() {
        MoodTag sleep = builtInTag("mood-tag-GOOD_SLEEP", "GOOD_SLEEP");
        when(tagRepository.findByIdAndDeletedAtIsNull("mood-tag-GOOD_SLEEP")).thenReturn(Optional.of(sleep));
        when(entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(MEMBER, TODAY_JAKARTA)).thenReturn(Optional.empty());

        MoodEntryResponse result = moodService.saveEntry(MEMBER, TODAY_JAKARTA,
                request(4, "  Hari ini produktif  ", "mood-tag-GOOD_SLEEP", "mood-tag-GOOD_SLEEP")).getData();

        ArgumentCaptor<MoodEntry> saved = ArgumentCaptor.forClass(MoodEntry.class);
        verify(entryRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getMemberId()).isEqualTo(MEMBER);
        assertThat(saved.getValue().getLocalDate()).isEqualTo(TODAY_JAKARTA);
        assertThat(saved.getValue().getMoodScore()).isEqualTo((short) 4);
        assertThat(saved.getValue().getNote()).isEqualTo("Hari ini produktif");
        assertThat(saved.getValue().getTags()).containsExactly(sleep);
        assertThat(saved.getValue().getCheckedInAt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        assertThat(result.getTags()).extracting("code").containsExactly("GOOD_SLEEP");
    }

    @Test
    void saveEntry_sameDayAgain_updatesTheExistingEntry() {
        MoodEntry existing = entry(2);
        when(entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(MEMBER, TODAY_JAKARTA)).thenReturn(Optional.of(existing));

        moodService.saveEntry(MEMBER, TODAY_JAKARTA, request(5, null));

        assertThat(existing.getMoodScore()).isEqualTo((short) 5);
        assertThat(existing.getNote()).isNull();
        assertThat(existing.getTags()).isEmpty();
        verify(entryRepository).saveAndFlush(existing);
    }

    @Test
    void saveEntry_concurrentInsertOfTheSameDay_retriesAsUpdate() {
        MoodEntry winner = entry(3);
        when(entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(MEMBER, TODAY_JAKARTA))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(entryRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uq_mood_entry_member_date"))
                .thenAnswer(inv -> inv.getArgument(0));

        MoodEntryResponse result = moodService.saveEntry(MEMBER, TODAY_JAKARTA, request(1, "x")).getData();

        assertThat(result.getMoodScore()).isEqualTo(1);
        verify(entryRepository, times(2)).saveAndFlush(any());
    }

    @Test
    void saveEntry_futureDayInTheMembersZone_isRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> moodService.saveEntry(MEMBER, TODAY_JAKARTA.plusDays(1), request(3, null)));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MOOD_DATE_IN_FUTURE);
        verify(entryRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveEntry_tagOfAnotherMember_isRejected() {
        MoodTag foreign = new MoodTag();
        foreign.setId("t-x");
        foreign.setMemberId("someone-else");
        when(tagRepository.findByIdAndDeletedAtIsNull("t-x")).thenReturn(Optional.of(foreign));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> moodService.saveEntry(MEMBER, TODAY_JAKARTA, request(3, null, "t-x")));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MOOD_TAG_NOT_FOUND);
    }

    // =========================================================================
    // read / delete
    // =========================================================================
    @Test
    void getEntries_defaultsToTheLast30DaysOfTheMember() {
        when(entryRepository.findRange(MEMBER, TODAY_JAKARTA.minusDays(29), TODAY_JAKARTA)).thenReturn(List.of(entry(4)));

        assertThat(moodService.getEntries(MEMBER, null, null).getData()).hasSize(1);
    }

    @Test
    void getToday_withoutEntry_returnsNullData() {
        when(entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(MEMBER, TODAY_JAKARTA)).thenReturn(Optional.empty());

        assertThat(moodService.getToday(MEMBER).getData()).isNull();
    }

    @Test
    void deleteEntry_softDeletes_andMissingDayIs404() {
        MoodEntry existing = entry(4);
        when(entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(MEMBER, TODAY_JAKARTA)).thenReturn(Optional.of(existing));
        when(entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(MEMBER, TODAY_JAKARTA.minusDays(1))).thenReturn(Optional.empty());

        moodService.deleteEntry(MEMBER, TODAY_JAKARTA);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThrows(NotFoundException.class, () -> moodService.deleteEntry(MEMBER, TODAY_JAKARTA.minusDays(1)));
    }

    // =========================================================================
    // tags
    // =========================================================================
    @Test
    void createTag_duplicateNameOrLimit_isRejected() {
        CreateMoodTagRequest request = new CreateMoodTagRequest();
        request.setName("Kopi");
        when(tagRepository.existsVisibleName(MEMBER, "Kopi")).thenReturn(true);

        assertThrows(ConflictException.class, () -> moodService.createTag(MEMBER, request));

        when(tagRepository.countByMemberIdAndDeletedAtIsNull(MEMBER)).thenReturn((long) MoodService.MAX_CUSTOM_TAGS);
        BadRequestException limit = assertThrows(BadRequestException.class, () -> moodService.createTag(MEMBER, request));
        assertThat(limit.getErrorConstant()).isEqualTo(ErrorConstant.MOOD_TAG_LIMIT_REACHED);
    }

    @Test
    void deleteTag_builtInOrForeign_is404() {
        when(tagRepository.findByIdAndDeletedAtIsNull("mood-tag-WORK")).thenReturn(Optional.of(builtInTag("mood-tag-WORK", "WORK")));

        assertThrows(NotFoundException.class, () -> moodService.deleteTag(MEMBER, "mood-tag-WORK"));
        verify(tagRepository, never()).save(any());
    }

    // =========================================================================
    // trends
    // =========================================================================
    @Test
    void trend_week_isOnePointPerDayOfTheCalendarWeek_andEmptyDaysAreNull() {
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        LocalDate monday = LocalDate.of(2026, 10, 19);
        when(entryRepository.findRange(MEMBER, monday, monday.plusDays(6))).thenReturn(List.of(
                entryOn(monday, 4), entryOn(monday.plusDays(2), 2), entryOn(TODAY_JAKARTA, 5)));

        MoodTrendResponse result = moodService.getTrend(MEMBER, TrendPeriod.WEEK, null).getData();

        assertThat(result.getFrom()).isEqualTo(monday);
        assertThat(result.getTo()).isEqualTo(monday.plusDays(6));
        assertThat(result.getPoints()).hasSize(7);
        assertThat(result.getPoints().get(0).getAverage()).isEqualByComparingTo("4");
        assertThat(result.getPoints().get(1).getAverage()).isNull(); // not 0
        assertThat(result.getPoints().get(1).getEntries()).isZero();
        assertThat(result.getAverage()).isEqualByComparingTo("3.67");
        assertThat(result.getEntries()).isEqualTo(3);
    }

    @Test
    void trend_month_isOnePointPerWeekCutToTheMonth() {
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        LocalDate first = LocalDate.of(2026, 10, 1);
        when(entryRepository.findRange(MEMBER, first, LocalDate.of(2026, 10, 31))).thenReturn(List.of(
                entryOn(LocalDate.of(2026, 10, 2), 3), entryOn(LocalDate.of(2026, 10, 3), 4)));

        MoodTrendResponse result = moodService.getTrend(MEMBER, TrendPeriod.MONTH, LocalDate.of(2026, 10, 15)).getData();

        // Thu 1 - Sun 4, 5-11, 12-18, 19-25, 26-31
        assertThat(result.getPoints()).extracting(p -> p.getFrom().getDayOfMonth()).containsExactly(1, 5, 12, 19, 26);
        assertThat(result.getPoints().get(0).getTo()).isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(result.getPoints().get(4).getTo()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(result.getPoints().get(0).getAverage()).isEqualByComparingTo("3.50");
        assertThat(result.getPoints().get(1).getAverage()).isNull();
    }

    @Test
    void tagsSummary_countsTagsMostUsedFirst() {
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        MoodTag work = builtInTag("t-work", "WORK");
        MoodTag family = builtInTag("t-family", "FAMILY");
        MoodEntry a = entryOn(TODAY_JAKARTA, 4);
        a.getTags().add(work);
        a.getTags().add(family);
        MoodEntry b = entryOn(TODAY_JAKARTA.minusDays(1), 2);
        b.getTags().add(work);
        MoodEntry c = entryOn(TODAY_JAKARTA.minusDays(2), 3);
        when(entryRepository.findRange(MEMBER, TODAY_JAKARTA.minusDays(29), TODAY_JAKARTA)).thenReturn(List.of(a, b, c));

        MoodTagsSummaryResponse result = moodService.getTagsSummary(MEMBER, null, null).getData();

        assertThat(result.getEntries()).isEqualTo(3);
        assertThat(result.getTags()).extracting(MoodTagCountResponse::getCode).containsExactly("WORK", "FAMILY");
        assertThat(result.getTags().get(0).getCount()).isEqualTo(2);
        assertThat(result.getTags().get(0).getPercent()).isEqualByComparingTo("66.7");
    }

    @Test
    void saveEntry_countsAsTheFirstMood() {
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
        when(entryRepository.saveAndFlush(any(MoodEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        moodService.saveEntry(MEMBER, TODAY_JAKARTA, request(4, null));

        verify(activationService).record(MEMBER, ActivationEvent.FIRST_MOOD);
    }

    private static MoodEntry entryOn(LocalDate day, int score) {
        MoodEntry entry = new MoodEntry();
        entry.setId("e-" + day);
        entry.setMemberId(MEMBER);
        entry.setLocalDate(day);
        entry.setMoodScore((short) score);
        return entry;
    }

    // ------------------------------------------------------------------
    private static MoodEntryRequest request(int score, String note, String... tagIds) {
        MoodEntryRequest request = new MoodEntryRequest();
        request.setMoodScore(score);
        request.setNote(note);
        request.setTagIds(List.of(tagIds));
        return request;
    }

    private static MoodEntry entry(int score) {
        MoodEntry entry = new MoodEntry();
        entry.setId("e-1");
        entry.setMemberId(MEMBER);
        entry.setLocalDate(TODAY_JAKARTA);
        entry.setMoodScore((short) score);
        return entry;
    }

    private static MoodTag builtInTag(String id, String code) {
        MoodTag tag = new MoodTag();
        tag.setId(id);
        tag.setCode(code);
        tag.setName(code);
        return tag;
    }
}
