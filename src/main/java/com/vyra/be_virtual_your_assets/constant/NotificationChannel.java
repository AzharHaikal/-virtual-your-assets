package com.vyra.be_virtual_your_assets.constant;

/** Where a notification is shown. EMAIL can be set but is not delivered yet. */
public enum NotificationChannel {
    IN_APP(true),
    PUSH(true),
    EMAIL(false);

    /** Used when the member has not chosen. */
    private final boolean enabledByDefault;

    NotificationChannel(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }
}
