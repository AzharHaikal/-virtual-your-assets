package com.vyra.virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "wallet", name = "wallet_statement_history")
@Entity
public class WalletStatementHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String walletStatementId;

    @Column(nullable = false)
    private String memberWalletId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal previousBalance;

    @Column(precision = 19, scale = 2)
    private BigDecimal credit;

    @Column(precision = 19, scale = 2)
    private BigDecimal debit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

}
