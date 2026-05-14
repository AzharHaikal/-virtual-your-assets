package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "transaction_finance", name = "transaction")
@Entity
public class Transaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false, length = 15)
    private String userPhoneNumber;

    @Column(nullable = false, length = 50)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    private TransactionCategory category;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String transactionDesc;

    @Column(nullable = false, unique = true, length = 50)
    private String referenceNumber;

    private LocalDateTime transactionDate;
}
