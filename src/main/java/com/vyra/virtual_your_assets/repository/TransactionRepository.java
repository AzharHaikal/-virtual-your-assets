package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findAllByMemberIdAndDeletedAtIsNullOrderByTransactionDateDesc(String memberId);

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.memberId = :memberId
        AND t.deletedAt IS NULL
        AND t.transactionDate BETWEEN :startDate AND :endDate
        AND (:type IS NULL OR t.type = :type)
    """)
    Page<Transaction> findTransactionHistory(
            @Param("memberId") String memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("type") String type,
            Pageable pageable
    );
}
