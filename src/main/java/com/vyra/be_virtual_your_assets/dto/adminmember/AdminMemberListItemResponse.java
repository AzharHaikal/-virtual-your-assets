package com.vyra.be_virtual_your_assets.dto.adminmember;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminMemberListItemResponse {
    private String memberId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private ActiveStatus status;
    private BigDecimal balance;
    private LocalDateTime joinDate;
}
