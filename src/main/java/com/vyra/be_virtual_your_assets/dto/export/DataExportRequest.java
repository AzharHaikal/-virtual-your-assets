package com.vyra.be_virtual_your_assets.dto.export;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

/** Exporting all personal data needs the PIN. */
@Data
public class DataExportRequest {
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must consist of exactly 6 digits")
    @ToString.Exclude
    private String pin;
}
