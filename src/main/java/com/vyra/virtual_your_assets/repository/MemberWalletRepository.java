package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.MemberWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberWalletRepository extends JpaRepository<MemberWallet, UUID> {

}
