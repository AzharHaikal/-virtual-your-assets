package com.vyra.be_virtual_your_assets.dto.mood;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MoodTagCountResponse {
    private String id;
    /** Built-in only; apps use it for the translated label. */
    private String code;
    private String name;
    private boolean custom;
    /** Entries with this tag. */
    private int count;
    /** Of the entries in the range, 1 decimal. */
    private BigDecimal percent;
}
