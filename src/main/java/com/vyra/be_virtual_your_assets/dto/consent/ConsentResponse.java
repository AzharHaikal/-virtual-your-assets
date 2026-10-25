package com.vyra.be_virtual_your_assets.dto.consent;

import com.vyra.be_virtual_your_assets.constant.ConsentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponse {
    private ConsentType type;
    private boolean granted;
    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;
}
