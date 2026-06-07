package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MemberWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberWalletRepository extends JpaRepository<MemberWallet, UUID> {
    Optional<MemberWallet> findByPhoneNumber(String phoneNumber);

}
