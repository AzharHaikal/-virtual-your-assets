package com.vyra.be_virtual_your_assets.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Where the confirmation code went. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountOtpResponse {
    private String email;
}
