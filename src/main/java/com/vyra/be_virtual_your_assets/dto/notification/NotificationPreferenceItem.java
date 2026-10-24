package com.vyra.be_virtual_your_assets.dto.notification;

import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceItem {
    @NotNull(message = "Type is required")
    private NotificationType type;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotNull(message = "Enabled is required")
    private Boolean enabled;
}
