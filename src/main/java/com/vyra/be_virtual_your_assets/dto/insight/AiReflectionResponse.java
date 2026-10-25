package com.vyra.be_virtual_your_assets.dto.insight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiReflectionResponse {
    private String id;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String content;
    private LocalDateTime createdAt;
}
