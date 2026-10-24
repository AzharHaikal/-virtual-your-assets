package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.finance.CategoryAmountResponse;
import com.vyra.be_virtual_your_assets.dto.finance.FinanceSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.finance.SavingsSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionSpecifications;
import com.vyra.be_virtual_your_assets.repository.TransactionSpecifications.SortField;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Member's transaction list with search / filters / sort, and the income-expense summary per category. */
@Service
@RequiredArgsConstructor
public class FinanceService {

    static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final CategoryService categoryService;
    private final ValidationService validationService;
    private final TimeService timeService;

    /**
     * @param sort "date" or "amount", optionally ",asc" / ",desc" (default "date,desc"); amount sorts by absolute value
     */
    @Transactional(readOnly = true)
    public BaseResponse<PageResponse<TransactionHistoryResponse>> listTransactions(
            String memberId, TransactionType type, String categoryId, LocalDate from, LocalDate to, String query,
            String sort, int page, int size) {
        requireRange(from, to);
        String[] sortParts = StringUtils.defaultIfBlank(sort, "date,desc").toLowerCase(Locale.ROOT).split(",");
        SortField field = switch (sortParts[0].trim()) {
            case "date" -> SortField.DATE;
            case "amount" -> SortField.AMOUNT;
            default -> throw new BadRequestException(ErrorConstant.INVALID_SORT);
        };
        String direction = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new BadRequestException(ErrorConstant.INVALID_SORT);
        }

        Page<Transaction> transactions = transactionRepository.findAll(
                TransactionSpecifications.memberTransactions(memberId, type, categoryId, from, to, query,
                        field, direction.equals("asc")),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));

        Map<String, FinanceCategory> categories = categoryService.byIds(
                transactions.getContent().stream().map(Transaction::getCategoryId).toList());
        Page<TransactionHistoryResponse> responses =
                transactions.map(t -> TransactionMapper.toHistoryResponse(t, categories));

        return new BaseResponse<>(ErrorConstant.TRANSACTION_LIST_SUCCESS.getCode(),
                ErrorConstant.TRANSACTION_LIST_SUCCESS.getMessage(), PageResponse.from(responses));
    }

    /** Defaults to the current calendar month in the member's zone. */
    @Transactional(readOnly = true)
    public BaseResponse<FinanceSummaryResponse> getSummary(String memberId, LocalDate from, LocalDate to) {
        Member member = validationService.getMemberById(memberId);
        LocalDate today = timeService.today(timeService.zoneOf(member));
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : today.withDayOfMonth(today.lengthOfMonth());
        requireRange(start, end);

        // A whole calendar month compares with the previous month, any other range with the same number of days before it.
        LocalDate previousTo = start.minusDays(1);
        LocalDate previousFrom = isWholeMonth(start, end)
                ? previousTo.withDayOfMonth(1)
                : previousTo.minusDays(ChronoUnit.DAYS.between(start, end));

        List<Object[]> rows = transactionRepository.sumByCategory(memberId, start, end);
        Map<String, FinanceCategory> categories = categoryService.byIds(
                rows.stream().map(r -> (String) r[0]).toList());

        List<CategoryAmountResponse> income = byCategory(rows, TransactionType.INCOME, categories);
        List<CategoryAmountResponse> expense = byCategory(rows, TransactionType.EXPENSE, categories);
        BigDecimal totalIncome = total(income);
        BigDecimal totalExpense = total(expense);
        withPercentages(income, totalIncome);
        withPercentages(expense, totalExpense);

        List<Object[]> previousRows = transactionRepository.sumByCategory(memberId, previousFrom, previousTo);

        FinanceSummaryResponse response = new FinanceSummaryResponse();
        response.setFrom(start);
        response.setTo(end);
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setNet(totalIncome.subtract(totalExpense));
        response.setPreviousFrom(previousFrom);
        response.setPreviousTo(previousTo);
        response.setPreviousIncome(sumOfType(previousRows, TransactionType.INCOME));
        response.setPreviousExpense(sumOfType(previousRows, TransactionType.EXPENSE));
        response.setIncomeByCategory(income);
        response.setExpenseByCategory(expense);
        response.setSavings(savings(memberId));
        return new BaseResponse<>(ErrorConstant.FINANCE_SUMMARY_SUCCESS.getCode(),
                ErrorConstant.FINANCE_SUMMARY_SUCCESS.getMessage(), response);
    }

    private SavingsSummaryResponse savings(String memberId) {
        List<Object[]> rows = goalRepository.sumSavingsTargets(memberId);
        if (rows.isEmpty() || rows.get(0) == null) {
            return new SavingsSummaryResponse(0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Object[] row = rows.get(0);
        return new SavingsSummaryResponse(((Number) row[0]).longValue(), toBigDecimal(row[1]), toBigDecimal(row[2]));
    }

    private static boolean isWholeMonth(LocalDate start, LocalDate end) {
        return start.getDayOfMonth() == 1 && end.equals(start.withDayOfMonth(start.lengthOfMonth()));
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(ErrorConstant.INVALID_DATE_RANGE);
        }
    }

    /** Rows: [categoryId, transactionType, signed sum]. */
    private static List<CategoryAmountResponse> byCategory(List<Object[]> rows, TransactionType type,
                                                           Map<String, FinanceCategory> categories) {
        List<CategoryAmountResponse> items = new ArrayList<>();
        for (Object[] row : rows) {
            if (!type.name().equals(String.valueOf(row[1]))) {
                continue;
            }
            BigDecimal amount = toBigDecimal(row[2]).abs();
            if (amount.signum() == 0) {
                continue;
            }
            CategoryAmountResponse item = new CategoryAmountResponse();
            item.setCategoryId((String) row[0]);
            FinanceCategory category = categories.get(item.getCategoryId());
            if (category != null) {
                item.setCode(category.getCode());
                item.setName(category.getName());
                item.setIcon(category.getIcon());
                item.setColor(category.getColor());
            }
            item.setAmount(amount);
            items.add(item);
        }
        items.sort(Comparator.comparing(CategoryAmountResponse::getAmount).reversed());
        return items;
    }

    private static void withPercentages(List<CategoryAmountResponse> items, BigDecimal total) {
        for (CategoryAmountResponse item : items) {
            item.setPercentage(total.signum() == 0 ? BigDecimal.ZERO
                    : item.getAmount().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP));
        }
    }

    private static BigDecimal total(List<CategoryAmountResponse> items) {
        return items.stream().map(CategoryAmountResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumOfType(List<Object[]> rows, TransactionType type) {
        return rows.stream()
                .filter(r -> type.name().equals(String.valueOf(r[1])))
                .map(r -> toBigDecimal(r[2]).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
