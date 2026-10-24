package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, String> {

    Optional<Budget> findByIdAndMemberIdAndDeletedAtIsNull(String id, String memberId);

    /** The member's budgets of the month starting on {@code periodStart}, not deleted, oldest first. */
    @Query("""
        SELECT b FROM Budget b
        WHERE b.memberId = :memberId AND b.periodStart = :periodStart AND b.deletedAt IS NULL
        ORDER BY b.createdAt, b.id
        """)
    List<Budget> findPeriod(@Param("memberId") String memberId, @Param("periodStart") LocalDate periodStart);

    boolean existsByMemberIdAndCategoryIdAndPeriodStartAndDeletedAtIsNull(String memberId, String categoryId,
                                                                          LocalDate periodStart);

    /** Active budgets of these categories whose period contains {@code day} (for the budget alerts). */
    @Query("""
        SELECT b FROM Budget b
        WHERE b.memberId = :memberId AND b.categoryId IN :categoryIds AND b.deletedAt IS NULL
          AND b.periodStart <= :day AND b.periodEnd >= :day
        """)
    List<Budget> findCovering(@Param("memberId") String memberId,
                              @Param("categoryIds") Collection<String> categoryIds,
                              @Param("day") LocalDate day);

    /** All the member's budgets, not deleted (data export). */
    List<Budget> findByMemberIdAndDeletedAtIsNullOrderByPeriodStartAscCreatedAtAsc(String memberId);

    /** A deleted category takes its budgets with it. */
    @Modifying
    @Query("""
        UPDATE Budget b SET b.deletedAt = :now, b.modifiedBy = :memberId
        WHERE b.memberId = :memberId AND b.categoryId = :categoryId AND b.deletedAt IS NULL
        """)
    int softDeleteByCategory(@Param("memberId") String memberId,
                             @Param("categoryId") String categoryId,
                             @Param("now") LocalDateTime now);
}
