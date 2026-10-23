package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.mood.CreateMoodTagRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTagResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.entity.MoodTag;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.MoodTagRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Mood check-in & journal. One active entry per member and local day (the member's time zone).
 * Private: only the member reads it; the journal is encrypted at rest and never logged.
 * Insights stay descriptive (no diagnosis).
 */
@Service
@Slf4j
public class MoodService {

    static final int MAX_CUSTOM_TAGS = 30;
    static final int DEFAULT_RANGE_DAYS = 30;

    private final MoodEntryRepository entryRepository;
    private final MoodTagRepository tagRepository;
    private final ValidationService validationService;
    private final TimeService timeService;
    private final TransactionTemplate transactionTemplate;

    public MoodService(MoodEntryRepository entryRepository, MoodTagRepository tagRepository,
                       ValidationService validationService, TimeService timeService,
                       PlatformTransactionManager transactionManager) {
        this.entryRepository = entryRepository;
        this.tagRepository = tagRepository;
        this.validationService = validationService;
        this.timeService = timeService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // =========================================================================
    // Entries
    // =========================================================================

    /** Creates or replaces the entry of {@code localDate}; the day must not be in the future (member's zone). */
    public BaseResponse<MoodEntryResponse> saveEntry(String memberId, LocalDate localDate, MoodEntryRequest request) {
        Member member = validationService.getMemberById(memberId);
        if (localDate.isAfter(timeService.today(timeService.zoneOf(member)))) {
            throw new BadRequestException(ErrorConstant.MOOD_DATE_IN_FUTURE);
        }
        MoodEntry saved;
        try {
            saved = transactionTemplate.execute(status -> upsert(memberId, localDate, request));
        } catch (DataIntegrityViolationException sameDayRace) {
            // Another check-in of the same day won the insert: update that one instead.
            saved = transactionTemplate.execute(status -> upsert(memberId, localDate, request));
        }
        return new BaseResponse<>(ErrorConstant.MOOD_SAVE_SUCCESS.getCode(),
                ErrorConstant.MOOD_SAVE_SUCCESS.getMessage(), toResponse(saved));
    }

    private MoodEntry upsert(String memberId, LocalDate localDate, MoodEntryRequest request) {
        Set<MoodTag> tags = resolveTags(memberId, request.getTagIds());
        LocalDateTime now = timeService.nowUtc();
        MoodEntry entry = entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(memberId, localDate)
                .orElseGet(() -> {
                    MoodEntry created = new MoodEntry();
                    created.setMemberId(memberId);
                    created.setLocalDate(localDate);
                    created.setCreatedBy(memberId);
                    created.setCreatedAt(now);
                    return created;
                });
        entry.setMoodScore(request.getMoodScore().shortValue());
        entry.setNote(StringUtils.trimToNull(request.getNote()));
        entry.getTags().clear();
        entry.getTags().addAll(tags);
        entry.setCheckedInAt(now);
        entry.setModifiedBy(memberId);
        entry.setUpdatedAt(now);
        return entryRepository.saveAndFlush(entry);
    }

    /** Entries of [from, to] (default: the last 30 days up to today), newest first. */
    @Transactional(readOnly = true)
    public BaseResponse<List<MoodEntryResponse>> getEntries(String memberId, LocalDate from, LocalDate to) {
        Member member = validationService.getMemberById(memberId);
        LocalDate today = timeService.today(timeService.zoneOf(member));
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new BadRequestException(ErrorConstant.INVALID_DATE_RANGE);
        }
        List<MoodEntryResponse> items = entryRepository.findRange(memberId, start, end).stream()
                .map(MoodService::toResponse)
                .toList();
        return new BaseResponse<>(ErrorConstant.MOOD_LIST_SUCCESS.getCode(),
                ErrorConstant.MOOD_LIST_SUCCESS.getMessage(), items);
    }

    /** Today's entry in the member's zone; {@code data} is null when there is none yet. */
    @Transactional(readOnly = true)
    public BaseResponse<MoodEntryResponse> getToday(String memberId) {
        Member member = validationService.getMemberById(memberId);
        LocalDate today = timeService.today(timeService.zoneOf(member));
        MoodEntryResponse data = entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(memberId, today)
                .map(MoodService::toResponse)
                .orElse(null);
        return new BaseResponse<>(ErrorConstant.MOOD_GET_SUCCESS.getCode(),
                ErrorConstant.MOOD_GET_SUCCESS.getMessage(), data);
    }

    @Transactional
    public BaseResponse<Void> deleteEntry(String memberId, LocalDate localDate) {
        MoodEntry entry = entryRepository.findByMemberIdAndLocalDateAndDeletedAtIsNull(memberId, localDate)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.MOOD_NOT_FOUND));
        entry.setDeletedAt(timeService.nowUtc());
        entry.setModifiedBy(memberId);
        entryRepository.save(entry);
        return new BaseResponse<>(ErrorConstant.MOOD_DELETE_SUCCESS.getCode(),
                ErrorConstant.MOOD_DELETE_SUCCESS.getMessage(), null);
    }

    // =========================================================================
    // Tags
    // =========================================================================

    @Transactional(readOnly = true)
    public BaseResponse<List<MoodTagResponse>> getTags(String memberId) {
        List<MoodTagResponse> items = tagRepository.findVisible(memberId).stream().map(MoodService::toResponse).toList();
        return new BaseResponse<>(ErrorConstant.MOOD_TAG_LIST_SUCCESS.getCode(),
                ErrorConstant.MOOD_TAG_LIST_SUCCESS.getMessage(), items);
    }

    @Transactional
    public BaseResponse<MoodTagResponse> createTag(String memberId, CreateMoodTagRequest request) {
        if (tagRepository.countByMemberIdAndDeletedAtIsNull(memberId) >= MAX_CUSTOM_TAGS) {
            throw new BadRequestException(ErrorConstant.MOOD_TAG_LIMIT_REACHED);
        }
        String name = request.getName().trim();
        if (tagRepository.existsVisibleName(memberId, name)) {
            throw new ConflictException(ErrorConstant.MOOD_TAG_NAME_EXISTS);
        }
        MoodTag tag = new MoodTag();
        tag.setMemberId(memberId);
        tag.setName(name);
        tag.setCreatedBy(memberId);
        tag.setCreatedAt(timeService.nowUtc());
        tagRepository.save(tag);
        return new BaseResponse<>(ErrorConstant.MOOD_TAG_CREATE_SUCCESS.getCode(),
                ErrorConstant.MOOD_TAG_CREATE_SUCCESS.getMessage(), toResponse(tag));
    }

    /** Soft delete of an own tag; past entries keep showing it. */
    @Transactional
    public BaseResponse<Void> deleteTag(String memberId, String tagId) {
        MoodTag tag = tagRepository.findByIdAndDeletedAtIsNull(tagId)
                .filter(t -> Objects.equals(t.getMemberId(), memberId))
                .orElseThrow(() -> new NotFoundException(ErrorConstant.MOOD_TAG_NOT_FOUND));
        tag.setDeletedAt(timeService.nowUtc());
        tag.setModifiedBy(memberId);
        tagRepository.save(tag);
        return new BaseResponse<>(ErrorConstant.MOOD_TAG_DELETE_SUCCESS.getCode(),
                ErrorConstant.MOOD_TAG_DELETE_SUCCESS.getMessage(), null);
    }

    private Set<MoodTag> resolveTags(String memberId, List<String> tagIds) {
        Set<MoodTag> tags = new LinkedHashSet<>();
        if (tagIds == null) {
            return tags;
        }
        for (String id : new LinkedHashSet<>(tagIds)) {
            MoodTag tag = tagRepository.findByIdAndDeletedAtIsNull(id)
                    .filter(t -> t.isBuiltIn() || Objects.equals(t.getMemberId(), memberId))
                    .orElseThrow(() -> new BadRequestException(ErrorConstant.MOOD_TAG_NOT_FOUND));
            tags.add(tag);
        }
        return tags;
    }

    static MoodEntryResponse toResponse(MoodEntry entry) {
        MoodEntryResponse response = new MoodEntryResponse();
        response.setId(entry.getId());
        response.setLocalDate(entry.getLocalDate());
        response.setMoodScore(entry.getMoodScore());
        response.setNote(entry.getNote());
        response.setCheckedInAt(entry.getCheckedInAt());
        response.setTags(entry.getTags().stream()
                .sorted(Comparator.comparingInt(MoodTag::getPosition).thenComparing(MoodTag::getName))
                .map(MoodService::toResponse)
                .toList());
        return response;
    }

    static MoodTagResponse toResponse(MoodTag tag) {
        MoodTagResponse response = new MoodTagResponse();
        response.setId(tag.getId());
        response.setCode(tag.getCode());
        response.setName(tag.getName());
        response.setCustom(!tag.isBuiltIn());
        return response;
    }
}
