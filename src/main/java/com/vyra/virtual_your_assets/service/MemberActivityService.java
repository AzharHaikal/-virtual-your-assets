package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.entity.MemberActivity;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberActivityService {
    private final MemberActivityRepository memberActivityRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createMemberActivity(String phoneNumber, String description) {
        MemberActivity request = MemberActivity.builder()
                .phoneNumber(phoneNumber)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        memberActivityRepository.save(request);
    }
}
