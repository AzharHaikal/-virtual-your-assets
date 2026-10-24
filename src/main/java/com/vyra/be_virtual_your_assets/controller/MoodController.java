package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.TrendPeriod;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.mood.CreateMoodTagRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTagResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTagsSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTrendResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.MoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Mood check-in, journal and tags. Only for the member themself (no admin access). */
@RestController
@RequiredArgsConstructor
public class MoodController {

    private final MoodService moodService;

    @GetMapping(ApiPath.V1_MOOD_ENTRIES)
    public BaseResponse<List<MoodEntryResponse>> getEntries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        return moodService.getEntries(memberId(authentication), from, to);
    }

    @GetMapping(ApiPath.V1_MOOD_ENTRIES + ApiPath.MOOD_TODAY)
    public BaseResponse<MoodEntryResponse> getToday(Authentication authentication) {
        return moodService.getToday(memberId(authentication));
    }

    /** period = week (default) or month; date = any day of it (default today, member's zone). */
    @GetMapping(ApiPath.V1_MOOD_ENTRIES + ApiPath.MOOD_TREND)
    public BaseResponse<MoodTrendResponse> getTrend(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return moodService.getTrend(memberId(authentication), TrendPeriod.parse(period), date);
    }

    /** from / to inclusive; default the last 30 days. */
    @GetMapping(ApiPath.V1_MOOD_ENTRIES + ApiPath.MOOD_TAGS_SUMMARY)
    public BaseResponse<MoodTagsSummaryResponse> getTagsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        return moodService.getTagsSummary(memberId(authentication), from, to);
    }

    /** Create or replace the entry of that day (yyyy-MM-dd, the member's local date). */
    @PutMapping(ApiPath.V1_MOOD_ENTRIES + ApiPath.MOOD_DATE)
    public BaseResponse<MoodEntryResponse> saveEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate,
            @Valid @RequestBody MoodEntryRequest request,
            Authentication authentication
    ) {
        return moodService.saveEntry(memberId(authentication), localDate, request);
    }

    @DeleteMapping(ApiPath.V1_MOOD_ENTRIES + ApiPath.MOOD_DATE)
    public BaseResponse<Void> deleteEntry(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate,
            Authentication authentication
    ) {
        return moodService.deleteEntry(memberId(authentication), localDate);
    }

    @GetMapping(ApiPath.V1_MOOD_TAGS)
    public BaseResponse<List<MoodTagResponse>> getTags(Authentication authentication) {
        return moodService.getTags(memberId(authentication));
    }

    @PostMapping(ApiPath.V1_MOOD_TAGS)
    public ResponseEntity<BaseResponse<MoodTagResponse>> createTag(
            @Valid @RequestBody CreateMoodTagRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moodService.createTag(memberId(authentication), request));
    }

    @DeleteMapping(ApiPath.V1_MOOD_TAGS + ApiPath.MOOD_TAG_ID)
    public BaseResponse<Void> deleteTag(@PathVariable String tagId, Authentication authentication) {
        return moodService.deleteTag(memberId(authentication), tagId);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
