package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCompletionRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.habit.UpdateHabitRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.HabitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 23);

    @Mock private HabitService habitService;

    @InjectMocks
    private HabitController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void habits_delegateWithTheAuthenticatedMember_andCreateIs201() {
        BaseResponse<List<HabitResponse>> list = new BaseResponse<>("VYRA-HBT-LST-000", "ok", List.of());
        when(habitService.getHabits("member-1", HabitCategory.HEALTH, true)).thenReturn(list);
        assertThat(controller.getHabits(HabitCategory.HEALTH, true, authentication)).isSameAs(list);

        BaseResponse<List<HabitResponse>> today = new BaseResponse<>("VYRA-HBT-TDY-000", "ok", List.of());
        when(habitService.getToday("member-1")).thenReturn(today);
        assertThat(controller.getToday(authentication)).isSameAs(today);

        CreateHabitRequest create = new CreateHabitRequest();
        BaseResponse<HabitResponse> created = new BaseResponse<>("VYRA-HBT-CRT-000", "ok", new HabitResponse());
        when(habitService.createHabit("member-1", create)).thenReturn(created);
        ResponseEntity<BaseResponse<HabitResponse>> response = controller.createHabit(create, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);

        UpdateHabitRequest update = new UpdateHabitRequest();
        BaseResponse<HabitResponse> updated = new BaseResponse<>("VYRA-HBT-UPD-000", "ok", null);
        when(habitService.updateHabit("member-1", "h-1", update)).thenReturn(updated);
        assertThat(controller.updateHabit("h-1", update, authentication)).isSameAs(updated);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-HBT-DEL-000", "ok", null);
        when(habitService.deleteHabit("member-1", "h-1")).thenReturn(deleted);
        assertThat(controller.deleteHabit("h-1", authentication)).isSameAs(deleted);
    }

    @Test
    void archiveAndCompletions_delegateWithTheAuthenticatedMember() {
        BaseResponse<HabitResponse> archived = new BaseResponse<>("VYRA-HBT-ARC-000", "ok", null);
        when(habitService.setArchived("member-1", "h-1", true)).thenReturn(archived);
        assertThat(controller.archive("h-1", authentication)).isSameAs(archived);

        BaseResponse<HabitResponse> restored = new BaseResponse<>("VYRA-HBT-UAR-000", "ok", null);
        when(habitService.setArchived("member-1", "h-1", false)).thenReturn(restored);
        assertThat(controller.unarchive("h-1", authentication)).isSameAs(restored);

        HabitCompletionRequest request = new HabitCompletionRequest();
        BaseResponse<HabitResponse> completed = new BaseResponse<>("VYRA-HBT-CMP-000", "ok", null);
        when(habitService.complete("member-1", "h-1", DAY, request)).thenReturn(completed);
        assertThat(controller.complete("h-1", DAY, request, authentication)).isSameAs(completed);

        BaseResponse<HabitResponse> removed = new BaseResponse<>("VYRA-HBT-UCM-000", "ok", null);
        when(habitService.uncomplete("member-1", "h-1", DAY)).thenReturn(removed);
        assertThat(controller.uncomplete("h-1", DAY, authentication)).isSameAs(removed);
    }
}
