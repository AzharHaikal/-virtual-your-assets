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
import java.time.LocalDate;
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
    AND t.localDate BETWEEN :startDate AND :endDate
    AND (:type IS NULL OR t.transactionType = :type)
    """)
    Page<Transaction> findTransactionHistory(
            @Param("memberId") String memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") TransactionType type,
            Pageable pageable
    );

    @Query(value = """
    SELECT COALESCE(SUM(t.amount), 0) AS net_balance
    FROM transaction_finance."transaction" t
    WHERE t.member_id = :memberId
        AND t.local_date >= :startDate
        AND t.local_date <= :endDate
        AND t.deleted_at IS NULL
    """, nativeQuery = true)
    BigDecimal getPreviousMonthIncome(
            @Param("memberId") String memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT
        t.local_date AS label,
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
        AND t.local_date >= :startDate
        AND t.deleted_at IS NULL
    GROUP BY t.local_date
    ORDER BY t.local_date
    """, nativeQuery = true)
    List<Object[]> getChartDaily(String memberId, LocalDate startDate);

    @Query(value = """
    SELECT
        TO_CHAR(CAST(DATE_TRUNC('week', t.local_date) AS date), 'YYYY-MM-DD') AS label,
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
        AND t.local_date >= :startDate
        AND t.deleted_at IS NULL
    GROUP BY CAST(DATE_TRUNC('week', t.local_date) AS date)
    ORDER BY CAST(DATE_TRUNC('week', t.local_date) AS date)
    """, nativeQuery = true)
    List<Object[]> getChartWeekly(String memberId, LocalDate startDate);

    @Query(value = """
    SELECT
        TO_CHAR(t.local_date, 'YYYY-MM') AS label,
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
        AND t.local_date >= :startDate
        AND t.deleted_at IS NULL
    GROUP BY TO_CHAR(t.local_date, 'YYYY-MM')
    ORDER BY TO_CHAR(t.local_date, 'YYYY-MM')
    """, nativeQuery = true)
    List<Object[]> getChartMonthly(
            String memberId,
            LocalDate startDate
    );

    @Query(value = """
    SELECT
        TO_CHAR(t.local_date, 'YYYY-MM') as label,
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
    GROUP BY TO_CHAR(t.local_date, 'YYYY-MM')
    ORDER BY TO_CHAR(t.local_date, 'YYYY-MM')
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
            TO_CHAR(t.local_date, 'YYYY-MM') AS label,
            COALESCE(SUM(CASE WHEN t.transaction_type = 'INCOME' THEN t.amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN t.transaction_type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS expense
        FROM transaction_finance.transaction t
        WHERE t.local_date >= :startDate
          AND t.deleted_at IS NULL
        GROUP BY TO_CHAR(t.local_date, 'YYYY-MM')
        ORDER BY TO_CHAR(t.local_date, 'YYYY-MM')
        """, nativeQuery = true)
    List<Object[]> getPlatformMonthlyChart(@Param("startDate") LocalDate startDate);

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
     * - Inclusive range of the members' local dates (transaction.local_date).
     * - Only the given statuses are returned (callers exclude INQUIRY/DELETED).
     * - No nullable parameters: pattern is a lower-cased LIKE pattern ("%" = everything).
     */
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.deletedAt IS NULL
        AND t.localDate >= :startDate
        AND t.localDate <= :endDate
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
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("types") List<TransactionType> types,
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    /**
     * Daily totals of SUCCESSFUL transactions of one type, aggregated in the
     * database so the result is exact for any date range. Days are the members'
     * local dates, range inclusive. Rows: [yyyy-MM-dd, total].
     */
    @Query(value = """
        SELECT TO_CHAR(t.local_date, 'YYYY-MM-DD') AS period,
               COALESCE(SUM(t.amount), 0) AS total
        FROM transaction_finance."transaction" t
        WHERE t.deleted_at IS NULL
          AND t.status = 'SUCCESS'
          AND t.transaction_type = :type
          AND t.local_date >= :startDate
          AND t.local_date <= :endDate
        GROUP BY t.local_date
        ORDER BY t.local_date
        """, nativeQuery = true)
    List<Object[]> sumPlatformAmountPerDay(
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
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
