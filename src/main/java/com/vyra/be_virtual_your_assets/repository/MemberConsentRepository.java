package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.ConsentType;
import com.vyra.be_virtual_your_assets.entity.MemberConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberConsentRepository extends JpaRepository<MemberConsent, MemberConsent.Key> {

    List<MemberConsent> findByMemberId(String memberId);

    Optional<MemberConsent> findByMemberIdAndConsentType(String memberId, ConsentType consentType);
}
