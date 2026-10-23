package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateMilestoneRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateMilestoneRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.GoalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalControllerTest {

    @Mock private GoalService goalService;

    @InjectMocks
    private GoalController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void goals_delegateWithTheAuthenticatedMember_andCreateIs201() {
        List<GoalStatus> statuses = List.of(GoalStatus.ACTIVE, GoalStatus.PAUSED);
        BaseResponse<List<GoalResponse>> list = new BaseResponse<>("VYRA-GOL-LST-000", "ok", List.of());
        when(goalService.getGoals("member-1", statuses)).thenReturn(list);
        assertThat(controller.getGoals(statuses, authentication)).isSameAs(list);

        BaseResponse<GoalResponse> detail = new BaseResponse<>("VYRA-GOL-DTL-000", "ok", new GoalResponse());
        when(goalService.getGoal("member-1", "g-1")).thenReturn(detail);
        assertThat(controller.getGoal("g-1", authentication)).isSameAs(detail);

        CreateGoalRequest create = new CreateGoalRequest();
        BaseResponse<GoalResponse> created = new BaseResponse<>("VYRA-GOL-CRT-000", "ok", new GoalResponse());
        when(goalService.createGoal("member-1", create)).thenReturn(created);
        ResponseEntity<BaseResponse<GoalResponse>> response = controller.createGoal(create, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);

        UpdateGoalRequest update = new UpdateGoalRequest();
        BaseResponse<GoalResponse> updated = new BaseResponse<>("VYRA-GOL-UPD-000", "ok", null);
        when(goalService.updateGoal("member-1", "g-1", update)).thenReturn(updated);
        assertThat(controller.updateGoal("g-1", update, authentication)).isSameAs(updated);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-GOL-DEL-000", "ok", null);
        when(goalService.deleteGoal("member-1", "g-1")).thenReturn(deleted);
        assertThat(controller.deleteGoal("g-1", authentication)).isSameAs(deleted);

        GoalProgressRequest progress = new GoalProgressRequest();
        BaseResponse<GoalResponse> saved = new BaseResponse<>("VYRA-GOL-PRG-000", "ok", null);
        when(goalService.addProgress("member-1", "g-1", progress)).thenReturn(saved);
        assertThat(controller.addProgress("g-1", progress, authentication)).isSameAs(saved);
    }

    @Test
    void milestones_delegateWithTheAuthenticatedMember_andAddIs201() {
        CreateMilestoneRequest create = new CreateMilestoneRequest();
        BaseResponse<GoalResponse> added = new BaseResponse<>("VYRA-GML-CRT-000", "ok", null);
        when(goalService.addMilestone("member-1", "g-1", create)).thenReturn(added);
        ResponseEntity<BaseResponse<GoalResponse>> response = controller.addMilestone("g-1", create, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(added);

        UpdateMilestoneRequest update = new UpdateMilestoneRequest();
        BaseResponse<GoalResponse> updated = new BaseResponse<>("VYRA-GML-UPD-000", "ok", null);
        when(goalService.updateMilestone("member-1", "g-1", "m-1", update)).thenReturn(updated);
        assertThat(controller.updateMilestone("g-1", "m-1", update, authentication)).isSameAs(updated);

        BaseResponse<GoalResponse> deleted = new BaseResponse<>("VYRA-GML-DEL-000", "ok", null);
        when(goalService.deleteMilestone("member-1", "g-1", "m-1")).thenReturn(deleted);
        assertThat(controller.deleteMilestone("g-1", "m-1", authentication)).isSameAs(deleted);
    }
}
