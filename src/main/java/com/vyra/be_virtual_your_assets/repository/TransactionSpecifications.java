package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filters for the member's transaction list (GET /api/v1/transactions). Only the filters that are set
 * are applied, so no nullable query parameters reach PostgreSQL.
 */
public final class TransactionSpecifications {

    public enum SortField { DATE, AMOUNT }

    private TransactionSpecifications() {
    }

    /**
     * The member's own, not deleted transactions matching the filters, ordered by {@code sortField}
     * (amount = absolute value, so the biggest expenses and incomes come first) and then newest first.
     */
    public static Specification<Transaction> memberTransactions(String memberId, TransactionType type, String categoryId,
                                                                LocalDate from, LocalDate to, String query,
                                                                SortField sortField, boolean ascending) {
        return (root, criteria, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("memberId"), memberId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.notEqual(root.get("status"), TransactionStatus.DELETED));
            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type));
            }
            if (StringUtils.isNotBlank(categoryId)) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("localDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("localDate"), to));
            }
            if (StringUtils.isNotBlank(query)) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("transactionDesc")), pattern),
                        cb.like(cb.lower(root.get("referenceNumber")), pattern)));
            }

            // The count query of a page must not be ordered.
            if (criteria.getResultType() != Long.class && criteria.getResultType() != long.class) {
                Expression<?> key = sortField == SortField.AMOUNT ? cb.abs(root.get("amount")) : root.get("transactionDate");
                criteria.orderBy(
                        ascending ? cb.asc(key) : cb.desc(key),
                        cb.desc(root.get("transactionDate")),
                        cb.desc(root.get("id")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
