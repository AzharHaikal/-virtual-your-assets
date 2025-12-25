package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.MemberActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberActivityRepository extends JpaRepository<MemberActivity, UUID> {
    void deleteByPhoneNumber(String phoneNumber);
}
