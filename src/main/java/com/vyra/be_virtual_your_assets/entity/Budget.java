package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.BudgetPeriodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A member's budget for one expense category and month. The amount spent is computed, never stored. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "transaction_finance", name = "budget")
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private String categoryId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BudgetPeriodType periodType = BudgetPeriodType.MONTHLY;

    /** First and last day of the month, the member's local dates. */
    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;
}
