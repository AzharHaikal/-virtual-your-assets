package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.entity.MemberActivity;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberActivityServiceTest {
    @Mock
    private MemberActivityRepository memberActivityRepository;
    @InjectMocks
    private MemberActivityService memberActivityService;

    @Test
    void createMemberActivitySuccess() {
        String phoneNumber = "62812345678";
        String description = "description";

        memberActivityService.createMemberActivity(phoneNumber, description);

        verify(memberActivityRepository, times(1)).save(any(MemberActivity.class));
    }
}