package com.vyra.be_virtual_your_assets.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadAllResponse {
    /** Notifications that were unread. */
    private int updated;
}
