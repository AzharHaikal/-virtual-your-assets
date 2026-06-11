package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.entity.MemberActivity;
import com.vyra.be_virtual_your_assets.repository.MemberActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberActivityServiceTest {

    @Mock
    private MemberActivityRepository memberActivityRepository;

    @InjectMocks
    private MemberActivityService memberActivityService;

    @Test
    void createMemberActivity_shouldSaveWithCorrectFields() {
        String phoneNumber = "628123456789";
        MemberActivityEvent event = MemberActivityEvent.ATTEMPT_LOGIN;

        memberActivityService.createMemberActivity(phoneNumber, event);

        ArgumentCaptor<MemberActivity> captor = ArgumentCaptor.forClass(MemberActivity.class);
        verify(memberActivityRepository).save(captor.capture());

        MemberActivity saved = captor.getValue();
        assertThat(saved.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(saved.getEvent()).isEqualTo(event.name());
        assertThat(saved.getDescription()).isEqualTo(event.getDescription());
        assertThat(saved.getCreatedBy()).isEqualTo(phoneNumber);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createMemberActivity_shouldWorkForEveryEvent() {
        for (MemberActivityEvent event : MemberActivityEvent.values()) {
            memberActivityService.createMemberActivity("628000000000", event);
        }
        verify(memberActivityRepository, times(MemberActivityEvent.values().length)).save(any(MemberActivity.class));
    }
}
