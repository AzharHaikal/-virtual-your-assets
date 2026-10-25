package com.vyra.be_virtual_your_assets.dto.consent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsentRequest {
    @NotNull(message = "granted is required")
    private Boolean granted;
}
