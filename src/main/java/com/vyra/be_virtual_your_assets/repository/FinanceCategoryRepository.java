package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, String> {

    /** Built-in categories and the member's own, not deleted: built-in first, then by position and name. */
    @Query("""
        SELECT c FROM FinanceCategory c
        WHERE (c.memberId IS NULL OR c.memberId = :memberId)
          AND c.deletedAt IS NULL
        ORDER BY CASE WHEN c.memberId IS NULL THEN 0 ELSE 1 END, c.position, LOWER(c.name)
        """)
    List<FinanceCategory> findVisible(@Param("memberId") String memberId);

    Optional<FinanceCategory> findByIdAndDeletedAtIsNull(String id);

    long countByMemberIdAndDeletedAtIsNull(String memberId);

    @Query("""
        SELECT COUNT(c) > 0 FROM FinanceCategory c
        WHERE c.memberId = :memberId
          AND c.type = :type
          AND LOWER(c.name) = LOWER(:name)
          AND c.deletedAt IS NULL
          AND c.id <> :excludeId
        """)
    boolean existsActiveName(@Param("memberId") String memberId,
                             @Param("type") TransactionType type,
                             @Param("name") String name,
                             @Param("excludeId") String excludeId);
}
