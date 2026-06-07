package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.virtual_your_assets.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Query("""
    SELECT t
    FROM Transaction t
    WHERE t.memberId = :memberId
    AND t.deletedAt IS NULL
    ORDER BY t.transactionDate DESC
    LIMIT 3
    """)
    List<Transaction> findTransactionHistory(@Param("memberId") String memberId);

    @Query("""
    SELECT t
    FROM Transaction t
    WHERE t.memberId = :memberId
    AND t.deletedAt IS NULL
    AND t.transactionDate BETWEEN :startDate AND :endDate
    AND (:type IS NULL OR t.transactionType = :type)
    """)
    Page<Transaction> findTransactionHistory(
            @Param("memberId") String memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("type") TransactionType type,
            Pageable pageable
    );

    @Query(value = """
    SELECT COALESCE(SUM(t.amount), 0) AS net_balance
    FROM transaction_finance."transaction" t
    WHERE t.member_id = :memberId
        AND t.transaction_date >= :startDate
        AND t.transaction_date <= :endDate
    """, nativeQuery = true)
    BigDecimal getPreviousMonthIncome(
            @Param("memberId") String memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
    SELECT
        DATE(t.transaction_date) AS label,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'INCOME'
                    THEN t.amount
                    ELSE 0
                END
            ),0
        ) AS income,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'EXPENSE'
                    THEN t.amount
                    ELSE 0
                END
            ),0
        ) AS expense
    FROM transaction_finance.transaction t
    WHERE t.member_id = :memberId
      AND t.transaction_date >= :startDate
      AND t.deleted_at IS NULL
    GROUP BY DATE(t.transaction_date)
    ORDER BY DATE(t.transaction_date)
    """, nativeQuery = true)
    List<Object[]> getChartDaily(String memberId, LocalDateTime startDate);

    @Query(value = """
    SELECT
        TO_CHAR(DATE_TRUNC('week', t.transaction_date), 'YYYY-MM-DD') AS label,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'INCOME'
                    THEN t.amount
                    ELSE 0
                END
            ),0
        ) AS income,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'EXPENSE'
                    THEN t.amount
                    ELSE 0
                END
            ),0
        ) AS expense
    FROM transaction_finance.transaction t
    WHERE t.member_id = :memberId
      AND t.transaction_date >= :startDate
      AND t.deleted_at IS NULL
    GROUP BY DATE_TRUNC('week', t.transaction_date)
    ORDER BY DATE_TRUNC('week', t.transaction_date)
    """, nativeQuery = true)
    List<Object[]> getChartWeekly(String memberId, LocalDateTime startDate);

    @Query(value = """
    SELECT
        TO_CHAR(
            t.transaction_date,
            'YYYY-MM'
        ) AS label,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'INCOME'
                    THEN t.amount
                    ELSE 0
                END
            ),0
        ) AS income,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'EXPENSE'
                    THEN t.amount
                    ELSE 0
                END
            ),0
        ) AS expense
    FROM transaction_finance.transaction t
    WHERE t.member_id = :memberId
      AND t.transaction_date >= :startDate
      AND t.deleted_at IS NULL
    GROUP BY
        TO_CHAR(
            t.transaction_date,
            'YYYY-MM'
        )
    ORDER BY
        TO_CHAR(
            t.transaction_date,
            'YYYY-MM'
        )
    """, nativeQuery = true)
    List<Object[]> getChartMonthly(
            String memberId,
            LocalDateTime startDate
    );

    @Query(value = """
    SELECT
        TO_CHAR(t.transaction_date, 'YYYY-MM') as label,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'INCOME'
                    THEN t.amount
                    ELSE 0
                END
            ),
            0
        ) as income,
        COALESCE(
            SUM(
                CASE
                    WHEN t.transaction_type = 'EXPENSE'
                    THEN t.amount
                    ELSE 0
                END
            ),
            0
        ) as expense
    FROM transaction_finance.transaction t
    WHERE t.member_id = :memberId
    AND t.deleted_at IS NULL
    GROUP BY TO_CHAR(t.transaction_date, 'YYYY-MM')
    ORDER BY TO_CHAR(t.transaction_date, 'YYYY-MM')
    """, nativeQuery = true)
    List<Object[]> getChartMonthlyAll(@Param("memberId") String memberId);

    List<Transaction> findAllByUserPhoneNumber(String oldPhoneNumber);
}
