package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.BudgetLevel;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetListResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CopyBudgetsResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CreateBudgetRequest;
import com.vyra.be_virtual_your_assets.dto.budget.UpdateBudgetRequest;
import com.vyra.be_virtual_your_assets.entity.Budget;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Monthly budgets per expense category. What is spent is summed from the member's SUCCESS expenses of
 * the category in the month (their local dates) every time it is read, so an edited or deleted
 * transaction is reflected right away.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    static final int WARNING_PERCENT = 80;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BudgetRepository budgetRepository;
    private final FinanceCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final ValidationService validationService;
    private final TimeService timeService;
    private final ActivationService activationService;

    // =========================================================================
    // Read
    // =========================================================================

    /** The budgets of {@code month} (default: the current month in the member's zone) with what is spent. */
    @Transactional(readOnly = true)
    public BaseResponse<BudgetListResponse> getBudgets(String memberId, YearMonth month) {
        YearMonth period = month != null ? month : currentMonth(memberId);
        return new BaseResponse<>(ErrorConstant.BUDGET_LIST_SUCCESS.getCode(),
                ErrorConstant.BUDGET_LIST_SUCCESS.getMessage(), list(memberId, period));
    }

    /** The budgets with what is spent of each; one query for the spending of all of them. */
    @Transactional(readOnly = true)
    public List<BudgetResponse> evaluate(String memberId, List<Budget> budgets) {
        if (budgets.isEmpty()) {
            return List.of();
        }
        LocalDate from = budgets.stream().map(Budget::getPeriodStart).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = budgets.stream().map(Budget::getPeriodEnd).max(LocalDate::compareTo).orElseThrow();
        boolean onePeriod = budgets.stream().allMatch(b -> b.getPeriodStart().equals(from) && b.getPeriodEnd().equals(to));

        Map<String, FinanceCategory> categories = categoryService.byIds(
                budgets.stream().map(Budget::getCategoryId).toList());
        Map<String, BigDecimal> spentOfPeriod = onePeriod ? spentByCategory(memberId, from, to) : null;

        List<BudgetResponse> items = new ArrayList<>(budgets.size());
        for (Budget budget : budgets) {
            Map<String, BigDecimal> spent = spentOfPeriod != null ? spentOfPeriod
                    : spentByCategory(memberId, budget.getPeriodStart(), budget.getPeriodEnd());
            items.add(toResponse(budget, categories.get(budget.getCategoryId()),
                    spent.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO)));
        }
        return items;
    }

    // =========================================================================
    // Write
    // =========================================================================

    @Transactional
    public BaseResponse<BudgetResponse> createBudget(String memberId, CreateBudgetRequest request) {
        YearMonth month = request.getMonth() != null ? request.getMonth() : currentMonth(memberId);
        FinanceCategory category = expenseCategory(memberId, request.getCategoryId());
        if (budgetRepository.existsByMemberIdAndCategoryIdAndPeriodStartAndDeletedAtIsNull(
                memberId, category.getId(), month.atDay(1))) {
            throw new ConflictException(ErrorConstant.BUDGET_ALREADY_EXISTS);
        }
        Budget budget = newBudget(memberId, category.getId(), request.getAmount(), month, timeService.nowUtc());
        try {
            budgetRepository.saveAndFlush(budget);
        } catch (DataIntegrityViolationException sameCategoryRace) {
            throw new ConflictException(ErrorConstant.BUDGET_ALREADY_EXISTS);
        }
        activationService.record(memberId, ActivationEvent.FIRST_BUDGET);
        return new BaseResponse<>(ErrorConstant.BUDGET_CREATE_SUCCESS.getCode(),
                ErrorConstant.BUDGET_CREATE_SUCCESS.getMessage(), evaluate(memberId, List.of(budget)).get(0));
    }

    @Transactional
    public BaseResponse<BudgetResponse> updateBudget(String memberId, String budgetId, UpdateBudgetRequest request) {
        Budget budget = findOwned(memberId, budgetId);
        budget.setAmount(request.getAmount());
        budget.setModifiedBy(memberId);
        budget.setUpdatedAt(timeService.nowUtc());
        budgetRepository.save(budget);
        return new BaseResponse<>(ErrorConstant.BUDGET_UPDATE_SUCCESS.getCode(),
                ErrorConstant.BUDGET_UPDATE_SUCCESS.getMessage(), evaluate(memberId, List.of(budget)).get(0));
    }

    /** Soft delete. */
    @Transactional
    public BaseResponse<Void> deleteBudget(String memberId, String budgetId) {
        Budget budget = findOwned(memberId, budgetId);
        budget.setDeletedAt(timeService.nowUtc());
        budget.setModifiedBy(memberId);
        budgetRepository.save(budget);
        return new BaseResponse<>(ErrorConstant.BUDGET_DELETE_SUCCESS.getCode(),
                ErrorConstant.BUDGET_DELETE_SUCCESS.getMessage(), null);
    }

    /**
     * Copies the budgets of the month before {@code month} into it. Categories that already have a budget
     * in {@code month}, or that were deleted, are skipped; running it twice copies nothing the second time.
     */
    @Transactional
    public BaseResponse<CopyBudgetsResponse> copyPrevious(String memberId, YearMonth month) {
        YearMonth target = month != null ? month : currentMonth(memberId);
        List<Budget> previous = budgetRepository.findPeriod(memberId, target.minusMonths(1).atDay(1));
        List<String> existing = budgetRepository.findPeriod(memberId, target.atDay(1)).stream()
                .map(Budget::getCategoryId).toList();
        Map<String, FinanceCategory> categories = categoryService.byIds(
                previous.stream().map(Budget::getCategoryId).toList());

        LocalDateTime now = timeService.nowUtc();
        List<Budget> copies = new ArrayList<>();
        for (Budget source : previous) {
            FinanceCategory category = categories.get(source.getCategoryId());
            if (existing.contains(source.getCategoryId()) || category == null || category.getDeletedAt() != null) {
                continue;
            }
            copies.add(newBudget(memberId, source.getCategoryId(), source.getAmount(), target, now));
        }
        budgetRepository.saveAll(copies);
        return new BaseResponse<>(ErrorConstant.BUDGET_COPY_SUCCESS.getCode(),
                ErrorConstant.BUDGET_COPY_SUCCESS.getMessage(),
                new CopyBudgetsResponse(copies.size(), previous.size() - copies.size(), list(memberId, target)));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private BudgetListResponse list(String memberId, YearMonth month) {
        List<BudgetResponse> items = evaluate(memberId, budgetRepository.findPeriod(memberId, month.atDay(1)));
        BudgetListResponse response = new BudgetListResponse();
        response.setMonth(month);
        response.setPeriodStart(month.atDay(1));
        response.setPeriodEnd(month.atEndOfMonth());
        response.setTotalBudget(items.stream().map(BudgetResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalSpent(items.stream().map(BudgetResponse::getSpent).reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalRemaining(response.getTotalBudget().subtract(response.getTotalSpent()));
        response.setItems(items);
        return response;
    }

    /** Expenses per category in [from, to], as positive amounts. */
    private Map<String, BigDecimal> spentByCategory(String memberId, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> spent = new HashMap<>();
        for (Object[] row : transactionRepository.sumByCategory(memberId, from, to)) {
            if (row[0] != null && TransactionType.EXPENSE.name().equals(String.valueOf(row[1]))) {
                spent.merge((String) row[0], new BigDecimal(String.valueOf(row[2])).abs(), BigDecimal::add);
            }
        }
        return spent;
    }

    static BudgetResponse toResponse(Budget budget, FinanceCategory category, BigDecimal spent) {
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setCategoryId(budget.getCategoryId());
        if (category != null) {
            response.setCategoryCode(category.getCode());
            response.setCategoryName(category.getName());
            response.setCategoryIcon(category.getIcon());
            response.setCategoryColor(category.getColor());
        }
        response.setPeriodStart(budget.getPeriodStart());
        response.setPeriodEnd(budget.getPeriodEnd());
        response.setAmount(budget.getAmount());
        response.setSpent(spent);
        response.setRemaining(budget.getAmount().subtract(spent));
        response.setPercent(percent(spent, budget.getAmount()));
        response.setLevel(level(spent, budget.getAmount()));
        return response;
    }

    /** Whole percent rounded down, so 80 / 100 only show once really reached. */
    static int percent(BigDecimal spent, BigDecimal amount) {
        return spent.multiply(HUNDRED).divide(amount, 0, RoundingMode.DOWN).intValue();
    }

    static BudgetLevel level(BigDecimal spent, BigDecimal amount) {
        if (spent.compareTo(amount) > 0) {
            return BudgetLevel.OVER;
        }
        return percent(spent, amount) >= WARNING_PERCENT ? BudgetLevel.WARNING : BudgetLevel.OK;
    }

    private Budget newBudget(String memberId, String categoryId, BigDecimal amount, YearMonth month, LocalDateTime now) {
        Budget budget = new Budget();
        budget.setMemberId(memberId);
        budget.setCategoryId(categoryId);
        budget.setAmount(amount);
        budget.setPeriodStart(month.atDay(1));
        budget.setPeriodEnd(month.atEndOfMonth());
        budget.setCreatedBy(memberId);
        budget.setCreatedAt(now);
        return budget;
    }

    /** Built-in or the member's own expense category, not deleted. */
    private FinanceCategory expenseCategory(String memberId, String categoryId) {
        FinanceCategory category = categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .filter(c -> c.isBuiltIn() || Objects.equals(c.getMemberId(), memberId))
                .orElseThrow(() -> new NotFoundException(ErrorConstant.CATEGORY_NOT_FOUND));
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BadRequestException(ErrorConstant.BUDGET_CATEGORY_NOT_EXPENSE);
        }
        return category;
    }

    private Budget findOwned(String memberId, String budgetId) {
        return budgetRepository.findByIdAndMemberIdAndDeletedAtIsNull(budgetId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.BUDGET_NOT_FOUND));
    }

    private YearMonth currentMonth(String memberId) {
        return YearMonth.from(timeService.today(timeService.zoneOf(validationService.getMemberById(memberId))));
    }
}
