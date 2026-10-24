package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.AppModule;
import com.vyra.be_virtual_your_assets.constant.AppTheme;
import com.vyra.be_virtual_your_assets.constant.DashboardCard;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.account.AccountResponse;
import com.vyra.be_virtual_your_assets.dto.account.DashboardCardSetting;
import com.vyra.be_virtual_your_assets.dto.account.UpdatePreferencesRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberPhoto;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MemberPhotoRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String MEMBER = "member-1";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 17, 30);
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0};

    @Mock private ValidationService validationService;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberPhotoRepository photoRepository;
    @Mock private ActivationService activationService;

    private AccountService accountService;
    private Member member;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        accountService = new AccountService(validationService, timeService, memberRepository, photoRepository,
                activationService, new ObjectMapper());
        member = new Member();
        member.setId(MEMBER);
        member.setFirstName("Azhar");
        member.setLastName("Haikal");
        member.setTimezone("Asia/Jakarta");
        member.setTheme(AppTheme.LIGHT);
        member.setLanguage(AppLanguage.EN);
        member.setCurrency("IDR");
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(photoRepository.findUpdatedAt(MEMBER)).thenReturn(Optional.empty());
    }

    @Test
    void getAccount_returnsProfileAndPreferences_withoutPhoto() {
        AccountResponse result = accountService.getAccount(MEMBER).getData();

        assertThat(result.getFullName()).isEqualTo("Azhar Haikal");
        assertThat(result.getTheme()).isEqualTo(AppTheme.LIGHT);
        assertThat(result.getCurrency()).isEqualTo("IDR");
        assertThat(result.getPhotoUpdatedAt()).isNull();
    }

    @Test
    void updatePreferences_changesOnlyTheSentFields_andNormalisesThem() {
        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setTheme(AppTheme.DARK);
        request.setTimezone(" Asia/Makassar ");
        request.setCurrency("usd");

        AccountResponse result = accountService.updatePreferences(MEMBER, request).getData();

        assertThat(result.getTheme()).isEqualTo(AppTheme.DARK);
        assertThat(result.getLanguage()).isEqualTo(AppLanguage.EN);
        assertThat(result.getTimezone()).isEqualTo("Asia/Makassar");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(member.getUpdatedAt()).isEqualTo(NOW);
        verify(memberRepository).save(member);
    }

    @Test
    void updatePreferences_invalidZoneOrCurrency_isRejected() {
        UpdatePreferencesRequest zone = new UpdatePreferencesRequest();
        zone.setTimezone("+07:00");
        assertError(ErrorConstant.INVALID_TIMEZONE, () -> accountService.updatePreferences(MEMBER, zone));

        UpdatePreferencesRequest currency = new UpdatePreferencesRequest();
        currency.setCurrency("XYZ");
        assertError(ErrorConstant.ACCOUNT_CURRENCY_UNSUPPORTED, () -> accountService.updatePreferences(MEMBER, currency));

        verify(memberRepository, never()).save(any());
        assertThat(member.getTimezone()).isEqualTo("Asia/Jakarta");
    }

    @Test
    void imageType_isTakenFromTheBytes() {
        assertThat(AccountService.imageType(JPEG)).isEqualTo("image/jpeg");
        assertThat(AccountService.imageType(PNG)).isEqualTo("image/png");
        assertThat(AccountService.imageType(WEBP)).isEqualTo("image/webp");
        assertThat(AccountService.imageType("<svg onload=x>".getBytes())).isNull();
        assertThat(AccountService.imageType(new byte[]{1})).isNull();
    }

    @Test
    void savePhoto_storesTheDetectedType_andReplacesTheOldPhoto() {
        MemberPhoto old = new MemberPhoto();
        old.setMemberId(MEMBER);
        old.setContent(JPEG);
        old.setContentType("image/jpeg");
        when(photoRepository.findById(MEMBER)).thenReturn(Optional.of(old));
        when(photoRepository.findUpdatedAt(MEMBER)).thenReturn(Optional.of(NOW));

        AccountResponse result = accountService.savePhoto(MEMBER, PNG).getData();

        ArgumentCaptor<MemberPhoto> saved = ArgumentCaptor.forClass(MemberPhoto.class);
        verify(photoRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue()).isSameAs(old);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getContent()).isEqualTo(PNG);
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);
        assertThat(result.getPhotoUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void savePhoto_emptyNotAnImageOrTooLarge_isRejected() {
        assertError(ErrorConstant.ACCOUNT_PHOTO_INVALID, () -> accountService.savePhoto(MEMBER, new byte[0]));
        assertError(ErrorConstant.ACCOUNT_PHOTO_INVALID, () -> accountService.savePhoto(MEMBER, "GIF89a".getBytes()));
        byte[] tooLarge = new byte[AccountService.MAX_PHOTO_BYTES + 1];
        System.arraycopy(JPEG, 0, tooLarge, 0, JPEG.length);
        assertError(ErrorConstant.ACCOUNT_PHOTO_TOO_LARGE, () -> accountService.savePhoto(MEMBER, tooLarge));
        verify(photoRepository, never()).saveAndFlush(any());
    }

    @Test
    void photo_missingIs404_andDeleteIsIdempotent() {
        when(photoRepository.findById(MEMBER)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> accountService.getPhoto(MEMBER));

        when(photoRepository.existsById(MEMBER)).thenReturn(false);
        accountService.deletePhoto(MEMBER);
        verify(photoRepository, never()).deleteById(any());

        when(photoRepository.existsById(MEMBER)).thenReturn(true);
        accountService.deletePhoto(MEMBER);
        verify(photoRepository).deleteById(MEMBER);
    }

    // ------------------------------------------------------------------
    private static void assertError(ErrorConstant expected, org.junit.jupiter.api.function.Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertThat(ex.getErrorConstant()).isEqualTo(expected);
    }

    // =========================================================================
    // onboarding & home screen (Phase 2.8)
    // =========================================================================
    @Test
    void newAccount_allModules_defaultLayout_onboardingNotDone() {
        AccountResponse result = accountService.getAccount(MEMBER).getData();

        assertThat(result.isOnboardingCompleted()).isFalse();
        assertThat(result.getEnabledModules()).containsExactly(AppModule.values());
        assertThat(result.getDashboardLayout()).extracting(DashboardCardSetting::getCard)
                .containsExactly(DashboardCard.values());
        assertThat(result.getDashboardLayout()).noneMatch(DashboardCardSetting::isHidden);
    }

    @Test
    void updatePreferences_modulesAndLayout_areStoredAndNormalised() {
        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setEnabledModules(List.of(AppModule.HABIT, AppModule.FINANCE, AppModule.HABIT));
        request.setDashboardLayout(List.of(
                new DashboardCardSetting(DashboardCard.GOALS, false),
                new DashboardCardSetting(DashboardCard.BALANCE, true),
                new DashboardCardSetting(DashboardCard.GOALS, true)));

        AccountResponse result = accountService.updatePreferences(MEMBER, request).getData();

        assertThat(member.getEnabledModules()).isEqualTo("FINANCE,HABIT");
        assertThat(result.getEnabledModules()).containsExactly(AppModule.FINANCE, AppModule.HABIT);
        assertThat(result.getDashboardLayout()).hasSize(DashboardCard.values().length);
        assertThat(result.getDashboardLayout().get(0)).isEqualTo(new DashboardCardSetting(DashboardCard.GOALS, false));
        assertThat(result.getDashboardLayout().get(1)).isEqualTo(new DashboardCardSetting(DashboardCard.BALANCE, true));
        // The stored JSON reads back the same.
        assertThat(accountService.getAccount(MEMBER).getData().getDashboardLayout()).isEqualTo(result.getDashboardLayout());

        UpdatePreferencesRequest none = new UpdatePreferencesRequest();
        none.setEnabledModules(List.of());
        assertError(ErrorConstant.ACCOUNT_MODULES_REQUIRED, () -> accountService.updatePreferences(MEMBER, none));
    }

    @Test
    void completeOnboarding_isIdempotent_andCountsOnce() {
        AccountResponse first = accountService.completeOnboarding(MEMBER).getData();
        assertThat(first.isOnboardingCompleted()).isTrue();
        assertThat(first.getOnboardingCompletedAt()).isEqualTo(NOW);

        accountService.completeOnboarding(MEMBER);
        verify(activationService, org.mockito.Mockito.times(1)).record(MEMBER, ActivationEvent.ONBOARDING_COMPLETED);
        assertThat(member.getOnboardingCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void storedLayoutThatCannotBeRead_fallsBackToTheDefault() {
        member.setDashboardLayout("not json");
        assertThat(accountService.getAccount(MEMBER).getData().getDashboardLayout())
                .extracting(DashboardCardSetting::getCard).containsExactly(DashboardCard.values());
        assertThat(AccountService.enabledModules("MOOD,REMOVED")).containsExactly(AppModule.MOOD);
    }
}
