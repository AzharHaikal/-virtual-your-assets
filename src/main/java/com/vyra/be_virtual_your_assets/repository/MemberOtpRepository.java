package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberOtpRepository extends JpaRepository<MemberOtp, UUID> {
    Optional<MemberOtp> findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(String phoneNumber, OtpType otpType);

    void deleteByPhoneNumberAndOtpType(String phoneNumber, OtpType otpType);

    void deleteByPhoneNumber(String phoneNumber);
}