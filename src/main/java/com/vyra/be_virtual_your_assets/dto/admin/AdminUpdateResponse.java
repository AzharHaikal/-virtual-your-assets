package com.vyra.be_virtual_your_assets.dto.admin;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import lombok.Data;

@Data
public class AdminUpdateResponse {
    private String adminId;
    private String name;
    private String email;
    private String phoneNumber;
    private AdminRole role;
    private ActiveStatus status;
}
