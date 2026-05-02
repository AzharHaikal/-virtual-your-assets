package com.vyra.virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "wallet", name = "wallet_statement_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatementHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String walletStatementHistoryId;

    @Column(name = "wallet_statement_id")
    private String walletStatementId;

    @Column(name = "member_wallet_id")
    private String memberWalletId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "previous_balance")
    private BigDecimal previousBalance;
    private BigDecimal credit;
    private BigDecimal debit;

    private String createdBy;
    private String modifiedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
