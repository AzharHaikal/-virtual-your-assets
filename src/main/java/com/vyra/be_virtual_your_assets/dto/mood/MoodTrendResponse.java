package com.vyra.be_virtual_your_assets.dto.mood;

import com.vyra.be_virtual_your_assets.constant.TrendPeriod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MoodTrendResponse {
    private TrendPeriod period;
    private LocalDate from;
    private LocalDate to;
    /** Over the whole period; null without entries. */
    private BigDecimal average;
    private int entries;
    private List<MoodTrendPointResponse> points;
}
