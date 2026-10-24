package com.vyra.be_virtual_your_assets.service;

import java.util.Map;

/** Sends one push notification to one device (FCM). Never logs the token or the texts. */
public interface PushClient {

    enum Result {
        SENT,
        /** The device token is no longer valid (app removed, token rotated): forget it. */
        INVALID_TOKEN,
        /** Temporary problem: try again later. */
        FAILED
    }

    Result send(String token, String title, String body, Map<String, String> data);

    /** false when push is not configured; deliveries then end as skipped. */
    boolean isEnabled();
}
