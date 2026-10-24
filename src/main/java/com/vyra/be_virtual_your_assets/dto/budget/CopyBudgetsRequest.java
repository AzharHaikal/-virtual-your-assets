package com.vyra.be_virtual_your_assets.dto.budget;

import lombok.Data;

import java.time.YearMonth;

@Data
public class CopyBudgetsRequest {
    /** The month to fill (yyyy-MM) from the month before it; default = the current month. */
    private YearMonth month;
}
