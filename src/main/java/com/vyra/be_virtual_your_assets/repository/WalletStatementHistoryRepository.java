package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.WalletStatementHistory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface WalletStatementHistoryRepository extends JpaRepository<WalletStatementHistory, String> {
}
