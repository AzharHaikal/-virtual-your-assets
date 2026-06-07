package com.vyra.virtual_your_assets.dto.wallet;

import lombok.Data;

@Data
public class WalletUpdateRequest {
    private String oldPhoneNumber;
    private String newPhoneNumber;
}