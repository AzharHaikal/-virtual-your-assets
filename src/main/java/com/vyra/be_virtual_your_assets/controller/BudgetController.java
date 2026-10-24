package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetListResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CopyBudgetsRequest;
import com.vyra.be_virtual_your_assets.dto.budget.CopyBudgetsResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CreateBudgetRequest;
import com.vyra.be_virtual_your_assets.dto.budget.UpdateBudgetRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Monthly budgets per expense category (Keuangan > Anggaran). */
@RestController
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /** month = yyyy-MM; default the current month in the member's zone. */
    @GetMapping(ApiPath.V1_BUDGETS)
    public BaseResponse<BudgetListResponse> getBudgets(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            Authentication authentication
    ) {
        return budgetService.getBudgets(memberId(authentication), month);
    }

    @PostMapping(ApiPath.V1_BUDGETS)
    public ResponseEntity<BaseResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(memberId(authentication), request));
    }

    @PatchMapping(ApiPath.V1_BUDGETS + ApiPath.BUDGET_ID)
    public BaseResponse<BudgetResponse> updateBudget(
            @PathVariable String budgetId,
            @Valid @RequestBody UpdateBudgetRequest request,
            Authentication authentication
    ) {
        return budgetService.updateBudget(memberId(authentication), budgetId, request);
    }

    @DeleteMapping(ApiPath.V1_BUDGETS + ApiPath.BUDGET_ID)
    public BaseResponse<Void> deleteBudget(@PathVariable String budgetId, Authentication authentication) {
        return budgetService.deleteBudget(memberId(authentication), budgetId);
    }

    @PostMapping(ApiPath.V1_BUDGETS + ApiPath.BUDGET_COPY_PREVIOUS)
    public BaseResponse<CopyBudgetsResponse> copyPrevious(
            @RequestBody(required = false) CopyBudgetsRequest request,
            Authentication authentication
    ) {
        return budgetService.copyPrevious(memberId(authentication), request != null ? request.getMonth() : null);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
