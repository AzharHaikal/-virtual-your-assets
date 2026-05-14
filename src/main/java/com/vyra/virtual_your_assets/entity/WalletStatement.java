package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.crypto.BalanceEncryptConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "wallet", name = "wallet_statement")
@Entity
public class WalletStatement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String memberWalletId;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalCredit;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalDebit;

    @Convert(converter = BalanceEncryptConverter.class)
    private BigDecimal balance;

}
