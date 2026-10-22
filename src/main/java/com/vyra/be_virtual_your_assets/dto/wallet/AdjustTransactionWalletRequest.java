package com.vyra.be_virtual_your_assets.dto.wallet;

import com.vyra.be_virtual_your_assets.constant.WalletEntryType;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Correction of a transaction already booked in the wallet. Amounts are signed as stored on the
 * transaction (income positive, expense negative). For a delete the new type is null and the new amount zero.
 */
@Value
@Builder
public class AdjustTransactionWalletRequest {
    String phoneNumber;
    String transactionId;
    TransactionType oldType;
    BigDecimal oldAmount;
    TransactionType newType;
    BigDecimal newAmount;
    WalletEntryType entryType;
}
