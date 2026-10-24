package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetListResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CopyBudgetsRequest;
import com.vyra.be_virtual_your_assets.dto.budget.CopyBudgetsResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CreateBudgetRequest;
import com.vyra.be_virtual_your_assets.dto.budget.UpdateBudgetRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    @Mock private BudgetService budgetService;

    @InjectMocks
    private BudgetController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void budgets_delegateWithTheAuthenticatedMember_andCreateIs201() {
        BaseResponse<BudgetListResponse> list = new BaseResponse<>("VYRA-BDG-LST-000", "ok", new BudgetListResponse());
        when(budgetService.getBudgets("member-1", MONTH)).thenReturn(list);
        assertThat(controller.getBudgets(MONTH, authentication)).isSameAs(list);

        CreateBudgetRequest create = new CreateBudgetRequest();
        BaseResponse<BudgetResponse> created = new BaseResponse<>("VYRA-BDG-CRT-000", "ok", new BudgetResponse());
        when(budgetService.createBudget("member-1", create)).thenReturn(created);
        ResponseEntity<BaseResponse<BudgetResponse>> response = controller.createBudget(create, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);

        UpdateBudgetRequest update = new UpdateBudgetRequest();
        BaseResponse<BudgetResponse> updated = new BaseResponse<>("VYRA-BDG-UPD-000", "ok", null);
        when(budgetService.updateBudget("member-1", "b-1", update)).thenReturn(updated);
        assertThat(controller.updateBudget("b-1", update, authentication)).isSameAs(updated);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-BDG-DEL-000", "ok", null);
        when(budgetService.deleteBudget("member-1", "b-1")).thenReturn(deleted);
        assertThat(controller.deleteBudget("b-1", authentication)).isSameAs(deleted);
    }

    @Test
    void copyPrevious_withOrWithoutABody() {
        BaseResponse<CopyBudgetsResponse> copied = new BaseResponse<>("VYRA-BDG-CPY-000", "ok", null);
        when(budgetService.copyPrevious("member-1", null)).thenReturn(copied);
        assertThat(controller.copyPrevious(null, authentication)).isSameAs(copied);

        CopyBudgetsRequest request = new CopyBudgetsRequest();
        request.setMonth(MONTH);
        when(budgetService.copyPrevious("member-1", MONTH)).thenReturn(copied);
        assertThat(controller.copyPrevious(request, authentication)).isSameAs(copied);
    }
}
