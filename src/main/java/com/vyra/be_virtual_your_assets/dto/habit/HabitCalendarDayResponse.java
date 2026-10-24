package com.vyra.be_virtual_your_assets.dto.habit;

import com.vyra.be_virtual_your_assets.constant.HabitDayStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HabitCalendarDayResponse {
    private LocalDate date;
    private HabitDayStatus status;
    private int count;
}
