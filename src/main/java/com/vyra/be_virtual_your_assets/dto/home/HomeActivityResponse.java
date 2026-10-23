package com.vyra.be_virtual_your_assets.dto.home;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One line of the home activity feed; only the fields of its type are set. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeActivityResponse {
    /** TRANSACTION, MOOD, HABIT or GOAL */
    private String type;
    /** Id of the transaction, mood entry, habit or goal. */
    private String refId;
    /** Category name, habit name or goal title; null for mood. */
    private String title;
    private String icon;
    private String color;
    /** UTC. */
    private LocalDateTime occurredAt;

    /** TRANSACTION: INCOME or EXPENSE, amount positive. */
    private String transactionType;
    /** TRANSACTION: amount; GOAL: value change (signed). */
    private BigDecimal amount;
    /** GOAL: unit of the value. */
    private String unit;
    /** GOAL: a note was added. */
    private Boolean hasNote;
    /** MOOD: 1..5. */
    private Integer moodScore;
    /** HABIT: progress of that day. */
    private Integer count;
    private Integer targetCount;
}
