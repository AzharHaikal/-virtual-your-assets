package com.vyra.be_virtual_your_assets.dto.transaction;

import lombok.Data;

import java.math.BigDecimal;

/** Result of an edit / delete: the transaction as it is now and the wallet after the correction. */
@Data
public class TransactionMutationResponse {
    private TransactionHistoryResponse transaction;
    private BigDecimal balance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
}
