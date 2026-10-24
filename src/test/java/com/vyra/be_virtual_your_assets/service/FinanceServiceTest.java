package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.finance.CategoryAmountResponse;
import com.vyra.be_virtual_your_assets.dto.finance.FinanceSummaryResponse;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    private static final String MEMBER = "member-1";

    @Mock private TransactionRepository transactionRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private CategoryService categoryService;
    @Mock private ValidationService validationService;
    /** 2026-10-22 17:30 UTC = 23 Oct in Jakarta. */
    @Spy private TimeService timeService =
            new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");

    @InjectMocks
    private FinanceService financeService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
    }

    // =========================================================================
    // listTransactions
    // =========================================================================
    @Test
    @SuppressWarnings("unchecked")
    void list_clampsPageSizeAndMapsCategories() {
        Transaction tx = new Transaction();
        tx.setId("t-1");
        tx.setCategoryId("c-1");
        tx.setAmount(new BigDecimal("-18000"));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        FinanceCategory kopi = new FinanceCategory();
        kopi.setId("c-1");
        kopi.setName("Kopi");
        when(categoryService.byIds(anyList())).thenReturn(Map.of("c-1", kopi));

        var result = financeService.listTransactions(MEMBER, TransactionType.EXPENSE, "c-1",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "kopi", "amount,asc", 0, 500);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.TRANSACTION_LIST_SUCCESS.getCode());
        assertThat(result.getData().getContent().get(0).getCategoryName()).isEqualTo("Kopi");
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(any(Specification.class), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(FinanceService.MAX_PAGE_SIZE);
    }

    @Test
    void list_invalidSortOrRange_isRejectedBeforeQuerying() {
        BadRequestException sort = assertThrows(BadRequestException.class, () -> financeService.listTransactions(
                MEMBER, null, null, null, null, null, "name", 0, 20));
        BadRequestException direction = assertThrows(BadRequestException.class, () -> financeService.listTransactions(
                MEMBER, null, null, null, null, null, "date,up", 0, 20));
        BadRequestException range = assertThrows(BadRequestException.class, () -> financeService.listTransactions(
                MEMBER, null, null, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null, null, 0, 20));

        assertThat(sort.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_SORT);
        assertThat(direction.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_SORT);
        assertThat(range.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_DATE_RANGE);
        verifyNoInteractions(transactionRepository);
    }

    // =========================================================================
    // getSummary
    // =========================================================================
    @Test
    void summary_defaultsToTheCurrentMonthOfTheMemberAndComparesWithThePreviousMonth() {
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(transactionRepository.sumByCategory(MEMBER, from, to)).thenReturn(List.of(
                new Object[]{"default-SALARY", "INCOME", new BigDecimal("5000000")},
                new Object[]{"default-FOOD_AND_BEVERAGE", "EXPENSE", new BigDecimal("-300000")},
                new Object[]{"c-1", "EXPENSE", new BigDecimal("-100000")},
                new Object[]{"default-TRANSPORTATION", "EXPENSE", new BigDecimal("0")}));
        when(transactionRepository.sumByCategory(MEMBER, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.<Object[]>of(new Object[]{"default-FOOD_AND_BEVERAGE", "EXPENSE", new BigDecimal("-250000")}));
        FinanceCategory kopi = new FinanceCategory();
        kopi.setId("c-1");
        kopi.setName("Kopi");
        kopi.setIcon("local_cafe");
        when(categoryService.byIds(anyList())).thenReturn(Map.of("c-1", kopi));

        FinanceSummaryResponse summary = financeService.getSummary(MEMBER, null, null).getData();

        assertThat(summary.getFrom()).isEqualTo(from);
        assertThat(summary.getTo()).isEqualTo(to);
        assertThat(summary.getTotalIncome()).isEqualByComparingTo("5000000");
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("400000");
        assertThat(summary.getNet()).isEqualByComparingTo("4600000");
        assertThat(summary.getPreviousExpense()).isEqualByComparingTo("250000");
        assertThat(summary.getPreviousIncome()).isEqualByComparingTo("0");

        List<CategoryAmountResponse> expense = summary.getExpenseByCategory();
        assertThat(expense).extracting(CategoryAmountResponse::getCategoryId)
                .containsExactly("default-FOOD_AND_BEVERAGE", "c-1"); // biggest first, zero rows dropped
        assertThat(expense.get(0).getPercentage()).isEqualByComparingTo("75.0");
        assertThat(expense.get(1).getName()).isEqualTo("Kopi");
        assertThat(expense.get(1).getPercentage()).isEqualByComparingTo("25.0");
        assertThat(summary.getIncomeByCategory().get(0).getPercentage()).isEqualByComparingTo("100.0");
        assertThat(summary.getSavings().getActiveGoals()).isZero();
        assertThat(summary.getSavings().getTotalTarget()).isEqualByComparingTo("0");
    }

    @Test
    void summary_includesTheActiveSavingsTargets() {
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
        when(goalRepository.sumSavingsTargets(MEMBER)).thenReturn(List.<Object[]>of(
                new Object[]{2L, new BigDecimal("15000000"), new BigDecimal("4500000")}));

        FinanceSummaryResponse summary = financeService.getSummary(MEMBER, null, null).getData();

        assertThat(summary.getSavings().getActiveGoals()).isEqualTo(2);
        assertThat(summary.getSavings().getTotalTarget()).isEqualByComparingTo("15000000");
        assertThat(summary.getSavings().getTotalSaved()).isEqualByComparingTo("4500000");
    }

    @Test
    void summary_customRange_previousPeriodHasTheSameLength() {
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
        when(transactionRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());

        FinanceSummaryResponse summary = financeService.getSummary(MEMBER,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 26)).getData();

        assertThat(summary.getPreviousFrom()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(summary.getPreviousTo()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("0");
        assertThat(summary.getExpenseByCategory()).isEmpty();
    }
}
