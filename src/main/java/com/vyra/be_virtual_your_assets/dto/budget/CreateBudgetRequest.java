package com.vyra.be_virtual_your_assets.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class CreateBudgetRequest {
    /** An expense category: built-in or the member's own. */
    @NotBlank(message = "Category is required")
    private String categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Invalid amount")
    private BigDecimal amount;

    /** yyyy-MM; default = the current month in the member's zone. */
    private YearMonth month;
}
