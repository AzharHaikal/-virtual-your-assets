package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MemberContactChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberContactChangeRepository extends JpaRepository<MemberContactChange, String> {
    Optional<MemberContactChange> findByMemberId(String memberId);

    void deleteByMemberId(String memberId);
}
