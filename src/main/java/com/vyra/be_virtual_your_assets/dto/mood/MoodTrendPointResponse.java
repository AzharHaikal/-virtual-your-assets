package com.vyra.be_virtual_your_assets.dto.mood;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day (period week) or one week (period month); no entry = null average, never 0. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodTrendPointResponse {
    private LocalDate from;
    private LocalDate to;
    /** Mean mood score 1..5 with 2 decimals; null without entries. */
    private BigDecimal average;
    private int entries;
}
