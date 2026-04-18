package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.crypto.BalanceEncryptConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_wallet")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberWalletId;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "total_credit")
    private BigDecimal totalCredit;

    @Column(name = "total_debit")
    private BigDecimal totalDebit;

    @Column(name = "balance")
    @Convert(converter = BalanceEncryptConverter.class)
    private BigDecimal balance;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
