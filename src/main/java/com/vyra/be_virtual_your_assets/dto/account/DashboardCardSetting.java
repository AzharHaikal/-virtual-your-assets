package com.vyra.be_virtual_your_assets.dto.account;

import com.vyra.be_virtual_your_assets.constant.DashboardCard;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One card of the home screen; the list order is the order on screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCardSetting {
    @NotNull(message = "Card is required")
    private DashboardCard card;

    private boolean hidden;
}
