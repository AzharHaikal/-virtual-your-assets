package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "wallet", name = "wallet_statement_history")
@Entity
public class WalletStatementHistory extends BaseEntity {
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

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal income = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 19, scale = 2)
    private BigDecimal expense = BigDecimal.ZERO;;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

}
