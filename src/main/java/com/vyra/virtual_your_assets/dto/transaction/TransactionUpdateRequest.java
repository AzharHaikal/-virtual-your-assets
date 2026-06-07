package com.vyra.virtual_your_assets.dto.transaction;

import lombok.Data;

@Data
public class TransactionUpdateRequest {
    private String oldPhoneNumber;
    private String newPhoneNumber;
    private String email;
}
