package com.vyra.be_virtual_your_assets.dto.account;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.AppModule;
import com.vyra.be_virtual_your_assets.constant.AppTheme;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** The signed-in member's profile and preferences (GET /api/v1/users/me). */
@Data
public class AccountResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String timezone;
    private AppTheme theme;
    private AppLanguage language;
    private String currency;
    /** null without a photo; changes when the photo changes, so the app can cache by it. */
    private LocalDateTime photoUpdatedAt;
    private LocalDateTime createdAt;

    /** false = show the onboarding. */
    private boolean onboardingCompleted;
    private LocalDateTime onboardingCompletedAt;
    /** The modules the member uses (all of them until chosen). */
    private List<AppModule> enabledModules;
    /** Every card of the home screen in order, with the hidden ones flagged. */
    private List<DashboardCardSetting> dashboardLayout;
}
