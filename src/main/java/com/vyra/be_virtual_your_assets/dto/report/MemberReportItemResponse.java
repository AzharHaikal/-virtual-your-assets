package com.vyra.be_virtual_your_assets.dto.report;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MemberReportItemResponse {
    private String memberId;
    private String fullName;
    private String email;
    private ActiveStatus status;
    private BigDecimal balance;
    private long totalTransactions;
    private LocalDateTime joinDate;
}
