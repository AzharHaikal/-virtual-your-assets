package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.BudgetLevel;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetListResponse;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CopyBudgetsResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CreateBudgetRequest;
import com.vyra.be_virtual_your_assets.dto.budget.UpdateBudgetRequest;
import com.vyra.be_virtual_your_assets.entity.Budget;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-31 17:30 UTC = Sunday 2026-11-01 00:30 in Jakarta: already November for the member. */
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @Mock private BudgetRepository budgetRepository;
    @Mock private FinanceCategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryService categoryService;
    @Mock private ValidationService validationService;
    @Mock private ActivationService activationService;

    private BudgetService budgetService;
    private final FinanceCategory food = category("default-FOOD_AND_BEVERAGE", null, TransactionType.EXPENSE);

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-31T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        budgetService = new BudgetService(budgetRepository, categoryRepository, transactionRepository, categoryService,
                validationService, timeService, activationService);
        Member member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(categoryService.byIds(anyCollection())).thenReturn(Map.of(food.getId(), food));
    }

    // =========================================================================
    // read
    // =========================================================================
    @Test
    void getBudgets_defaultsToTheMembersCurrentMonth_andSumsOnlyExpensesOfTheCategory() {
        Budget budget = budget("b-1", food.getId(), "1000000", NOVEMBER);
        when(budgetRepository.findPeriod(MEMBER, NOVEMBER.atDay(1))).thenReturn(List.of(budget));
        when(transactionRepository.sumByCategory(MEMBER, NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth())).thenReturn(List.of(
                new Object[]{food.getId(), "EXPENSE", new BigDecimal("-850000")},
                new Object[]{"default-SALARY", "INCOME", new BigDecimal("5000000")}));

        BudgetListResponse result = budgetService.getBudgets(MEMBER, null).getData();

        assertThat(result.getMonth()).isEqualTo(NOVEMBER);
        assertThat(result.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 11, 30));
        BudgetResponse item = result.getItems().get(0);
        assertThat(item.getSpent()).isEqualByComparingTo("850000");
        assertThat(item.getRemaining()).isEqualByComparingTo("150000");
        assertThat(item.getPercent()).isEqualTo(85);
        assertThat(item.getLevel()).isEqualTo(BudgetLevel.WARNING);
        assertThat(item.getCategoryName()).isEqualTo(food.getName());
        assertThat(result.getTotalBudget()).isEqualByComparingTo("1000000");
        assertThat(result.getTotalSpent()).isEqualByComparingTo("850000");
    }

    @Test
    void level_warnsFrom80Percent_andIsOverOnlyAboveTheBudget() {
        BigDecimal amount = new BigDecimal("100");
        assertThat(BudgetService.level(new BigDecimal("79.99"), amount)).isEqualTo(BudgetLevel.OK);
        assertThat(BudgetService.level(new BigDecimal("80"), amount)).isEqualTo(BudgetLevel.WARNING);
        assertThat(BudgetService.level(new BigDecimal("100"), amount)).isEqualTo(BudgetLevel.WARNING);
        assertThat(BudgetService.level(new BigDecimal("100.01"), amount)).isEqualTo(BudgetLevel.OVER);
        assertThat(BudgetService.percent(new BigDecimal("250"), amount)).isEqualTo(250);
    }

    @Test
    void evaluate_aDeletedOrEditedTransaction_isNoLongerCounted() {
        // The spending comes straight from the SUCCESS, not deleted transactions every time it is read.
        Budget budget = budget("b-1", food.getId(), "500000", OCTOBER);
        when(transactionRepository.sumByCategory(MEMBER, OCTOBER.atDay(1), OCTOBER.atEndOfMonth()))
                .thenReturn(List.<Object[]>of(new Object[]{food.getId(), "EXPENSE", new BigDecimal("-600000")}))
                .thenReturn(List.of());

        assertThat(budgetService.evaluate(MEMBER, List.of(budget)).get(0).getLevel()).isEqualTo(BudgetLevel.OVER);
        assertThat(budgetService.evaluate(MEMBER, List.of(budget)).get(0).getSpent()).isEqualByComparingTo("0");
    }

    // =========================================================================
    // create / update / delete
    // =========================================================================
    @Test
    void createBudget_forAnExpenseCategory_inTheCurrentMonth() {
        when(categoryRepository.findByIdAndDeletedAtIsNull(food.getId())).thenReturn(Optional.of(food));
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(food.getId());
        request.setAmount(new BigDecimal("1500000"));

        BudgetResponse result = budgetService.createBudget(MEMBER, request).getData();

        ArgumentCaptor<Budget> saved = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPeriodStart()).isEqualTo(NOVEMBER.atDay(1));
        assertThat(saved.getValue().getPeriodEnd()).isEqualTo(NOVEMBER.atEndOfMonth());
        assertThat(saved.getValue().getMemberId()).isEqualTo(MEMBER);
        assertThat(result.getAmount()).isEqualByComparingTo("1500000");
        assertThat(result.getLevel()).isEqualTo(BudgetLevel.OK);
        verify(activationService).record(MEMBER, ActivationEvent.FIRST_BUDGET);
    }

    @Test
    void createBudget_incomeCategory_othersCategory_orDuplicate_isRejected() {
        FinanceCategory salary = category("default-SALARY", null, TransactionType.INCOME);
        when(categoryRepository.findByIdAndDeletedAtIsNull("default-SALARY")).thenReturn(Optional.of(salary));
        BadRequestException income = assertThrows(BadRequestException.class,
                () -> budgetService.createBudget(MEMBER, create("default-SALARY", OCTOBER)));
        assertThat(income.getErrorConstant()).isEqualTo(ErrorConstant.BUDGET_CATEGORY_NOT_EXPENSE);

        FinanceCategory foreign = category("c-x", "member-2", TransactionType.EXPENSE);
        when(categoryRepository.findByIdAndDeletedAtIsNull("c-x")).thenReturn(Optional.of(foreign));
        assertThrows(NotFoundException.class, () -> budgetService.createBudget(MEMBER, create("c-x", OCTOBER)));

        when(categoryRepository.findByIdAndDeletedAtIsNull(food.getId())).thenReturn(Optional.of(food));
        when(budgetRepository.existsByMemberIdAndCategoryIdAndPeriodStartAndDeletedAtIsNull(
                MEMBER, food.getId(), OCTOBER.atDay(1))).thenReturn(true);
        ConflictException duplicate = assertThrows(ConflictException.class,
                () -> budgetService.createBudget(MEMBER, create(food.getId(), OCTOBER)));
        assertThat(duplicate.getErrorConstant()).isEqualTo(ErrorConstant.BUDGET_ALREADY_EXISTS);
        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void createBudget_parallelDuplicate_isAConflictToo() {
        when(categoryRepository.findByIdAndDeletedAtIsNull(food.getId())).thenReturn(Optional.of(food));
        when(budgetRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_budget"));

        assertThrows(ConflictException.class, () -> budgetService.createBudget(MEMBER, create(food.getId(), OCTOBER)));
    }

    @Test
    void updateAndDelete_onlyTheOwnBudget() {
        Budget budget = budget("b-1", food.getId(), "100", OCTOBER);
        when(budgetRepository.findByIdAndMemberIdAndDeletedAtIsNull("b-1", MEMBER)).thenReturn(Optional.of(budget));
        UpdateBudgetRequest update = new UpdateBudgetRequest();
        update.setAmount(new BigDecimal("250"));

        assertThat(budgetService.updateBudget(MEMBER, "b-1", update).getData().getAmount()).isEqualByComparingTo("250");
        budgetService.deleteBudget(MEMBER, "b-1");
        assertThat(budget.getDeletedAt()).isNotNull();

        NotFoundException missing = assertThrows(NotFoundException.class,
                () -> budgetService.deleteBudget(MEMBER, "b-other"));
        assertThat(missing.getErrorConstant()).isEqualTo(ErrorConstant.BUDGET_NOT_FOUND);
    }

    // =========================================================================
    // copy previous month
    // =========================================================================
    @Test
    @SuppressWarnings("unchecked")
    void copyPrevious_skipsExistingAndDeletedCategories() {
        FinanceCategory deleted = category("c-gone", MEMBER, TransactionType.EXPENSE);
        deleted.setDeletedAt(java.time.LocalDateTime.of(2026, 10, 15, 0, 0));
        FinanceCategory transport = category("default-TRANSPORTATION", null, TransactionType.EXPENSE);
        when(categoryService.byIds(anyCollection())).thenReturn(Map.of(
                food.getId(), food, deleted.getId(), deleted, transport.getId(), transport));
        when(budgetRepository.findPeriod(MEMBER, OCTOBER.atDay(1))).thenReturn(List.of(
                budget("b-1", food.getId(), "100", OCTOBER),
                budget("b-2", deleted.getId(), "200", OCTOBER),
                budget("b-3", transport.getId(), "300", OCTOBER)));
        when(budgetRepository.findPeriod(MEMBER, NOVEMBER.atDay(1)))
                .thenReturn(List.of(budget("b-4", food.getId(), "150", NOVEMBER)));

        CopyBudgetsResponse result = budgetService.copyPrevious(MEMBER, null).getData();

        ArgumentCaptor<List<Budget>> saved = ArgumentCaptor.forClass(List.class);
        verify(budgetRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(b -> {
            assertThat(b.getCategoryId()).isEqualTo(transport.getId());
            assertThat(b.getAmount()).isEqualByComparingTo("300");
            assertThat(b.getPeriodStart()).isEqualTo(NOVEMBER.atDay(1));
        });
        assertThat(result.getCopied()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    private static CreateBudgetRequest create(String categoryId, YearMonth month) {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryId);
        request.setAmount(new BigDecimal("100000"));
        request.setMonth(month);
        return request;
    }

    private static Budget budget(String id, String categoryId, String amount, YearMonth month) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setMemberId(MEMBER);
        budget.setCategoryId(categoryId);
        budget.setAmount(new BigDecimal(amount));
        budget.setPeriodStart(month.atDay(1));
        budget.setPeriodEnd(month.atEndOfMonth());
        return budget;
    }

    private static FinanceCategory category(String id, String memberId, TransactionType type) {
        FinanceCategory category = new FinanceCategory();
        category.setId(id);
        category.setMemberId(memberId);
        category.setName(id);
        category.setType(type);
        category.setIcon("restaurant");
        category.setColor("#F97316");
        return category;
    }
}
