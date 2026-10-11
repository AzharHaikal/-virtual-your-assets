package com.vyra.be_virtual_your_assets.dto.dashboard;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LatestMemberResponse {
    private String memberId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private ActiveStatus status;
    private LocalDateTime joinDate;
}
