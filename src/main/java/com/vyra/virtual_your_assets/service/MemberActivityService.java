package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
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
    public void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        MemberActivity request = MemberActivity.builder()
                .phoneNumber(phoneNumber)
                .event(activityEvent)
                .description(activityEvent.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        memberActivityRepository.save(request);
    }
}
