package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.entity.MemberActivity;
import com.vyra.be_virtual_your_assets.repository.MemberActivityRepository;
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
        MemberActivity memberActivity = new MemberActivity();
        memberActivity.setPhoneNumber(phoneNumber);
        memberActivity.setEvent(activityEvent.name());
        memberActivity.setDescription(activityEvent.getDescription());
        memberActivity.setCreatedBy(phoneNumber);
        memberActivity.setCreatedAt(LocalDateTime.now());
        memberActivityRepository.save(memberActivity);
    }
}
