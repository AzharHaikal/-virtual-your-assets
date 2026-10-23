package com.vyra.be_virtual_your_assets.dto.goal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** A change of the value (VALUE goals only, negative to correct) and/or a note; at least one of them. */
@Data
public class GoalProgressRequest {
    @Digits(integer = 17, fraction = 2, message = "Invalid value")
    private BigDecimal delta;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
