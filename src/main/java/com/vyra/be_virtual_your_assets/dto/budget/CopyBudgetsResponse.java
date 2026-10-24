package com.vyra.be_virtual_your_assets.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyBudgetsResponse {
    private int copied;
    /** Categories that already had a budget in the month, or that were deleted since. */
    private int skipped;
    private BudgetListResponse budgets;
}
