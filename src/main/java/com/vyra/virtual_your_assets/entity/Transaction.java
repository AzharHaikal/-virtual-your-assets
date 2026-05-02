package com.vyra.virtual_your_assets.entity;

import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "transaction_finance", name = "transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String transactionId;

    @Column(name = "user_phone_number", nullable = false)
    private String userPhoneNumber;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    private TransactionCategory category;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "transaction_desc")
    private String transactionDesc;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    private String createdBy;
    private String modifiedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
