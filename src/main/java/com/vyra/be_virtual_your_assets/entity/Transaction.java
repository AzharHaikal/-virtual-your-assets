package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.transaction.PaymentMethod;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.vyra.be_virtual_your_assets.service.TimeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "transaction_finance", name = "transaction",
        indexes = {
                @Index(name = "idx_transaction_member_date", columnList = "member_id, transaction_date"),
                @Index(name = "idx_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_transaction_type_status_date", columnList = "transaction_type, status, transaction_date"),
                @Index(name = "idx_transaction_user_phone", columnList = "user_phone_number"),
                @Index(name = "idx_transaction_member_local_date", columnList = "member_id, local_date"),
                @Index(name = "idx_transaction_local_date", columnList = "local_date")
        })
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

    /** Legacy code (built-in category code, or OTHERS_* for a custom one); see {@link #categoryId}. */
    private String transactionCategory;

    /** finance_category.id (built-in "default-&lt;CODE&gt;" or the member's own category). */
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String transactionDesc;

    @Column(nullable = false, unique = true, length = 50)
    private String referenceNumber;

    /** Optional. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /** UTC instant of the transaction. */
    private LocalDateTime transactionDate;

    /** The member's local date of {@link #transactionDate}, fixed when written (see TimeService). */
    @Column(nullable = false)
    private LocalDate localDate;

    /** Rows created outside TransactionService (seeders) fall back to the default zone. */
    @PrePersist
    void defaultLocalDate() {
        if (localDate == null && transactionDate != null) {
            localDate = transactionDate.atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(ZoneId.of(TimeService.FALLBACK_ZONE_ID)).toLocalDate();
        }
    }
}
