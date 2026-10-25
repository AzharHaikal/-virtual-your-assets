package com.vyra.be_virtual_your_assets.dto.status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Public status for the app (also before signing in): maintenance mode and its message. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppStatusResponse {
    private boolean maintenance;
    /** null when not in maintenance. */
    private String message;
    private LocalDateTime serverTime;
}
