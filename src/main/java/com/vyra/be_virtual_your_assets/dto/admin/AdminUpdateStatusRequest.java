package com.vyra.be_virtual_your_assets.dto.admin;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateStatusRequest {
    @NotNull(message = "Status is required")
    private ActiveStatus status;
}
