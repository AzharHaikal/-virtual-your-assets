package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findAllByMemberIdAndDeletedAtIsNullOrderByTransactionDateDesc(String memberId);
}
