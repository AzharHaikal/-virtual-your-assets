package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
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
        AND t.deleted_at IS NULL
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
    WHERE 
        t.member_id = :memberId
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
    WHERE 
        t.member_id = :memberId
        AND t.transaction_date >= :startDate
        AND t.deleted_at IS NULL
    GROUP BY DATE_TRUNC('week', t.transaction_date)
    ORDER BY DATE_TRUNC('week', t.transaction_date)
    """, nativeQuery = true)
    List<Object[]> getChartWeekly(String memberId, LocalDateTime startDate);

    @Query(value = """
    SELECT
        TO_CHAR(t.transaction_date, 'YYYY-MM') AS label,
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
    WHERE 
        t.member_id = :memberId
        AND t.transaction_date >= :startDate
        AND t.deleted_at IS NULL
    GROUP BY TO_CHAR(t.transaction_date, 'YYYY-MM')
    ORDER BY TO_CHAR(t.transaction_date, 'YYYY-MM')
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
    WHERE 
        t.member_id = :memberId
        AND t.deleted_at IS NULL
    GROUP BY TO_CHAR(t.transaction_date, 'YYYY-MM')
    ORDER BY TO_CHAR(t.transaction_date, 'YYYY-MM')
    """, nativeQuery = true)
    List<Object[]> getChartMonthlyAll(@Param("memberId") String memberId);

    List<Transaction> findAllByUserPhoneNumber(String oldPhoneNumber);

    // =========================================================================
    // ADMIN PORTAL QUERIES (platform-wide, not scoped to a single member)
    // =========================================================================

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.deletedAt IS NULL
        ORDER BY t.transactionDate DESC
        LIMIT 10
        """)
    List<Transaction> findRecentTransactions();

    long countByDeletedAtIsNull();

    /** Transaction counts for many members in one query. Rows: [memberId, count]. */
    @Query("""
        SELECT t.memberId, COUNT(t)
        FROM Transaction t
        WHERE t.memberId IN :memberIds
        AND t.deletedAt IS NULL
        GROUP BY t.memberId
        """)
    List<Object[]> countByMemberIds(@Param("memberIds") java.util.Collection<String> memberIds);

    @Query(value = """
        SELECT
            DATE_TRUNC('month', t.transaction_date) AS label,
            COALESCE(SUM(CASE WHEN t.transaction_type = 'INCOME' THEN t.amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN t.transaction_type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS expense
        FROM transaction_finance.transaction t
        WHERE t.transaction_date >= :startDate
          AND t.deleted_at IS NULL
        GROUP BY DATE_TRUNC('month', t.transaction_date)
        ORDER BY DATE_TRUNC('month', t.transaction_date)
        """, nativeQuery = true)
    List<Object[]> getPlatformMonthlyChart(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
        SELECT t.transaction_category AS category, COALESCE(SUM(ABS(t.amount)), 0) AS total
        FROM transaction_finance.transaction t
        WHERE t.deleted_at IS NULL
        GROUP BY t.transaction_category
        ORDER BY total DESC
        """, nativeQuery = true)
    List<Object[]> getCategoryDistribution();

    /**
     * Platform-wide transaction ledger for the portal report.
     * - Half-open window [startDate, endDate): pass the first instant AFTER the last day.
     * - Only the given statuses are returned (callers exclude INQUIRY/DELETED).
     * - No nullable parameters: pattern is a lower-cased LIKE pattern ("%" = everything).
     */
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.deletedAt IS NULL
        AND t.transactionDate >= :startDate
        AND t.transactionDate < :endDate
        AND t.transactionType IN :types
        AND t.status IN :statuses
        AND (
            LOWER(COALESCE(t.userEmail, '')) LIKE :pattern
            OR LOWER(COALESCE(t.referenceNumber, '')) LIKE :pattern
            OR COALESCE(t.userPhoneNumber, '') LIKE :pattern
        )
        ORDER BY t.transactionDate DESC
        """)
    Page<Transaction> findPlatformTransactions(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("types") List<TransactionType> types,
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    /**
     * Daily totals of SUCCESSFUL transactions of one type, aggregated in the
     * database so the result is exact for any date range. Rows: [yyyy-MM-dd, total].
     */
    @Query(value = """
        SELECT TO_CHAR(DATE(t.transaction_date), 'YYYY-MM-DD') AS period,
               COALESCE(SUM(t.amount), 0) AS total
        FROM transaction_finance."transaction" t
        WHERE t.deleted_at IS NULL
          AND t.status = 'SUCCESS'
          AND t.transaction_type = :type
          AND t.transaction_date >= :startDate
          AND t.transaction_date < :endDate
        GROUP BY DATE(t.transaction_date)
        ORDER BY DATE(t.transaction_date)
        """, nativeQuery = true)
    List<Object[]> sumPlatformAmountPerDay(
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.memberId = :memberId
        AND t.deletedAt IS NULL
        ORDER BY t.transactionDate DESC
        """)
    Page<Transaction> findByMemberIdForAdmin(@Param("memberId") String memberId, Pageable pageable);
}
