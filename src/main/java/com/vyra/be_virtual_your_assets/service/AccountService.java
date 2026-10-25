package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.AppCurrency;
import com.vyra.be_virtual_your_assets.constant.AppModule;
import com.vyra.be_virtual_your_assets.constant.DashboardCard;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.account.AccountResponse;
import com.vyra.be_virtual_your_assets.dto.account.DashboardCardSetting;
import com.vyra.be_virtual_your_assets.dto.account.UpdatePreferencesRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberPhoto;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MemberPhotoRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** The signed-in member's own account: profile, preferences shared by all devices and profile photo. */
@Service
@RequiredArgsConstructor
public class AccountService {

    /** The app compresses the photo first; this is a hard upper bound. */
    public static final int MAX_PHOTO_BYTES = 1024 * 1024;

    private final ValidationService validationService;
    private final TimeService timeService;
    private final MemberRepository memberRepository;
    private final MemberPhotoRepository photoRepository;
    private final ActivationService activationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BaseResponse<AccountResponse> getAccount(String memberId) {
        Member member = validationService.getMemberById(memberId);
        return new BaseResponse<>(ErrorConstant.ACCOUNT_GET_SUCCESS.getCode(),
                ErrorConstant.ACCOUNT_GET_SUCCESS.getMessage(), toResponse(member));
    }

    @Transactional
    public BaseResponse<AccountResponse> updatePreferences(String memberId, UpdatePreferencesRequest request) {
        Member member = validationService.getMemberById(memberId);
        if (request.getTheme() != null) {
            member.setTheme(request.getTheme());
        }
        if (request.getLanguage() != null) {
            member.setLanguage(request.getLanguage());
        }
        if (request.getTimezone() != null) {
            // Only data written from now on uses the new zone; existing local dates stay as they are.
            member.setTimezone(timeService.requireValidZoneId(request.getTimezone()));
        }
        if (request.getCurrency() != null) {
            String currency = request.getCurrency().trim().toUpperCase(Locale.ROOT);
            if (!AppCurrency.SUPPORTED.contains(currency)) {
                throw new BadRequestException(ErrorConstant.ACCOUNT_CURRENCY_UNSUPPORTED);
            }
            member.setCurrency(currency);
        }
        if (request.getEnabledModules() != null) {
            if (request.getEnabledModules().isEmpty()) {
                throw new BadRequestException(ErrorConstant.ACCOUNT_MODULES_REQUIRED);
            }
            member.setEnabledModules(EnumSet.copyOf(request.getEnabledModules()).stream()
                    .map(Enum::name).collect(Collectors.joining(",")));
        }
        if (request.getDashboardLayout() != null) {
            member.setDashboardLayout(writeLayout(normalizeLayout(request.getDashboardLayout())));
        }
        member.setModifiedBy(memberId);
        member.setUpdatedAt(timeService.nowUtc());
        memberRepository.save(member);
        return new BaseResponse<>(ErrorConstant.ACCOUNT_PREFERENCES_SUCCESS.getCode(),
                ErrorConstant.ACCOUNT_PREFERENCES_SUCCESS.getMessage(), toResponse(member));
    }

    /** Marks the onboarding as done (idempotent: the first time is kept). */
    @Transactional
    public BaseResponse<AccountResponse> completeOnboarding(String memberId) {
        Member member = validationService.getMemberById(memberId);
        if (member.getOnboardingCompletedAt() == null) {
            member.setOnboardingCompletedAt(timeService.nowUtc());
            member.setModifiedBy(memberId);
            member.setUpdatedAt(timeService.nowUtc());
            memberRepository.save(member);
            activationService.record(memberId, ActivationEvent.ONBOARDING_COMPLETED);
        }
        return new BaseResponse<>(ErrorConstant.ONBOARDING_COMPLETE_SUCCESS.getCode(),
                ErrorConstant.ONBOARDING_COMPLETE_SUCCESS.getMessage(), toResponse(member));
    }

    /**
     * Replaces the profile photo. The type is taken from the image bytes, not from the request,
     * so only real JPEG / PNG / WebP images are stored.
     */
    @Transactional
    public BaseResponse<AccountResponse> savePhoto(String memberId, byte[] content) {
        Member member = validationService.getMemberById(memberId);
        if (content == null || content.length == 0) {
            throw new BadRequestException(ErrorConstant.ACCOUNT_PHOTO_INVALID);
        }
        if (content.length > MAX_PHOTO_BYTES) {
            throw new BadRequestException(ErrorConstant.ACCOUNT_PHOTO_TOO_LARGE);
        }
        String contentType = imageType(content);
        if (contentType == null) {
            throw new BadRequestException(ErrorConstant.ACCOUNT_PHOTO_INVALID);
        }
        MemberPhoto photo = photoRepository.findById(memberId).orElseGet(() -> {
            MemberPhoto created = new MemberPhoto();
            created.setMemberId(memberId);
            return created;
        });
        photo.setContent(content);
        photo.setContentType(contentType);
        photo.setUpdatedAt(timeService.nowUtc());
        photoRepository.saveAndFlush(photo);
        return new BaseResponse<>(ErrorConstant.ACCOUNT_PHOTO_SUCCESS.getCode(),
                ErrorConstant.ACCOUNT_PHOTO_SUCCESS.getMessage(), toResponse(member));
    }

    @Transactional(readOnly = true)
    public MemberPhoto getPhoto(String memberId) {
        return photoRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.ACCOUNT_PHOTO_NOT_FOUND));
    }

    /** Idempotent. */
    @Transactional
    public BaseResponse<AccountResponse> deletePhoto(String memberId) {
        Member member = validationService.getMemberById(memberId);
        if (photoRepository.existsById(memberId)) {
            photoRepository.deleteById(memberId);
            photoRepository.flush();
        }
        return new BaseResponse<>(ErrorConstant.ACCOUNT_PHOTO_DELETE_SUCCESS.getCode(),
                ErrorConstant.ACCOUNT_PHOTO_DELETE_SUCCESS.getMessage(), toResponse(member));
    }

    /** JPEG, PNG or WebP by their magic bytes; null for anything else. */
    static String imageType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private AccountResponse toResponse(Member member) {
        AccountResponse response = new AccountResponse();
        response.setId(member.getId());
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setFullName((member.getFirstName() + " " + member.getLastName()).trim());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setTimezone(member.getTimezone());
        response.setTheme(member.getTheme());
        response.setLanguage(member.getLanguage());
        response.setCurrency(member.getCurrency());
        response.setPhotoUpdatedAt(photoRepository.findUpdatedAt(member.getId()).orElse(null));
        response.setCreatedAt(member.getCreatedAt());
        response.setOnboardingCompleted(member.getOnboardingCompletedAt() != null);
        response.setOnboardingCompletedAt(member.getOnboardingCompletedAt());
        response.setEnabledModules(enabledModules(member.getEnabledModules()));
        response.setDashboardLayout(normalizeLayout(readLayout(member.getDashboardLayout())));
        return response;
    }

    static List<AppModule> enabledModules(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of(AppModule.values());
        }
        List<AppModule> modules = new ArrayList<>();
        for (String name : stored.split(",")) {
            try {
                modules.add(AppModule.valueOf(name.trim()));
            } catch (IllegalArgumentException ignored) {
                // A module that no longer exists.
            }
        }
        return modules.isEmpty() ? List.of(AppModule.values()) : modules;
    }

    /** The given order without duplicates, then every missing card (shown) in the default order. */
    static List<DashboardCardSetting> normalizeLayout(List<DashboardCardSetting> layout) {
        Map<DashboardCard, DashboardCardSetting> cards = new LinkedHashMap<>();
        for (DashboardCardSetting setting : layout) {
            if (setting != null && setting.getCard() != null) {
                cards.putIfAbsent(setting.getCard(), new DashboardCardSetting(setting.getCard(), setting.isHidden()));
            }
        }
        for (DashboardCard card : DashboardCard.values()) {
            cards.putIfAbsent(card, new DashboardCardSetting(card, false));
        }
        return new ArrayList<>(cards.values());
    }

    private List<DashboardCardSetting> readLayout(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            // Cards that were removed later (e.g. QUICK_ACTIONS) are read as null and skipped.
            return objectMapper.readerFor(new TypeReference<List<DashboardCardSetting>>() { })
                    .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                    .readValue(json);
        } catch (JsonProcessingException e) {
            // A layout the app cannot use: fall back to the default one.
            return List.of();
        }
    }

    private String writeLayout(List<DashboardCardSetting> layout) {
        try {
            return objectMapper.writeValueAsString(layout);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Dashboard layout is not serialisable", e);
        }
    }
}
