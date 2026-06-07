package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletStatementRepository extends JpaRepository<WalletStatement, UUID> {
    Optional<WalletStatement> findByPhoneNumber(String phoneNumber);

}
