package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalDueJobTest {

    @Mock private GoalRepository goalRepository;
    @Mock private NotificationService notificationService;

    private GoalDueJob job;

    @BeforeEach
    void setUp() {
        job = new GoalDueJob(goalRepository, notificationService, new TimeService(Clock.systemUTC(), "Asia/Jakarta"));
    }

    @Test
    void notifiesThreeDaysBefore_from8InTheMembersZone() {
        Member member = member();
        Goal goal = goal(LocalDate.of(2026, 10, 25));
        // 01:05 UTC = 08:05 WIB on 2026-10-22: due in 3 days.
        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 1, 5);
        when(goalRepository.findActiveDueBetween(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 26)))
                .thenReturn(List.<Object[]>of(new Object[]{goal, member}));
        when(notificationService.notify(any(), any(), any(), any(), any())).thenReturn(true);

        assertThat(job.run(now)).isEqualTo(1);
        verify(notificationService).notify(eq(member), eq(NotificationType.GOAL_DUE), eq("GOAL_DUE:g-1:2026-10-25"), any(),
                anyMap());
    }

    @Test
    void before8_orNotExactly3DaysAway_waits() {
        Member member = member();
        // 00:05 UTC = 07:05 WIB: too early.
        when(goalRepository.findActiveDueBetween(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{goal(LocalDate.of(2026, 10, 25)), member}));
        assertThat(job.run(LocalDateTime.of(2026, 10, 22, 0, 5))).isZero();

        // 08:05 WIB but due in 4 days.
        when(goalRepository.findActiveDueBetween(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{goal(LocalDate.of(2026, 10, 26)), member}));
        assertThat(job.run(LocalDateTime.of(2026, 10, 22, 1, 5))).isZero();
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    private static Member member() {
        Member member = new Member();
        member.setId("m-1");
        member.setTimezone("Asia/Jakarta");
        member.setLanguage(AppLanguage.EN);
        return member;
    }

    private static Goal goal(LocalDate dueDate) {
        Goal goal = new Goal();
        goal.setId("g-1");
        goal.setTitle("Laptop");
        goal.setDueDate(dueDate);
        return goal;
    }
}
