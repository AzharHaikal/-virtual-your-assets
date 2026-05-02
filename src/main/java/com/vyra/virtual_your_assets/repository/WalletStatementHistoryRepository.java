package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.WalletStatementHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletStatementHistoryRepository extends JpaRepository<WalletStatementHistory, UUID> {
}
