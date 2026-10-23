package com.vyra.be_virtual_your_assets.dto.habit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** One day of the current week (Monday .. Sunday in the member's zone). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HabitDayResponse {
    private LocalDate localDate;
    private boolean scheduled;
    private int count;
    private boolean done;
}
