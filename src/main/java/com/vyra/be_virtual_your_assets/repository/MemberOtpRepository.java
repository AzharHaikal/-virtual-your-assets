package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberOtpRepository extends JpaRepository<MemberOtp, String> {
    Optional<MemberOtp> findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(String phoneNumber, OtpType otpType);

    /**
     * The latest OTP of the type, locked until the caller's transaction ends. Verification counts wrong
     * guesses on this row: without the lock, parallel guesses all read the same count and the attempt
     * limit could be bypassed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM MemberOtp o
        WHERE o.phoneNumber = :phoneNumber AND o.otpType = :otpType
        ORDER BY o.createdAt DESC
        LIMIT 1
        """)
    Optional<MemberOtp> findLatestForUpdate(@Param("phoneNumber") String phoneNumber, @Param("otpType") OtpType otpType);

    void deleteByPhoneNumberAndOtpType(String phoneNumber, OtpType otpType);

    void deleteByPhoneNumber(String phoneNumber);
}