package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.entity.Budget;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.event.TransactionChangedEvent;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Budget alerts: after a transaction change is committed, every budget it touches is checked; reaching
 * 80% or 100% notifies the member once per budget and level (dedup key BUDGET:{budgetId}:80 / :100).
 * Failing here never affects the transaction itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertService {

    static final int ALERT_PERCENT = 80;
    static final int FULL_PERCENT = 100;

    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    private final ValidationService validationService;
    private final NotificationService notificationService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransactionChanged(TransactionChangedEvent event) {
        try {
            check(event);
        } catch (RuntimeException e) {
            log.warn("Budget alert check failed. memberId: {} reason: {}", event.memberId(), e.getClass().getSimpleName());
        }
    }

    void check(TransactionChangedEvent event) {
        if (event.categoryIds().isEmpty()) {
            return;
        }
        Map<String, Budget> budgets = new LinkedHashMap<>();
        for (LocalDate day : event.localDates()) {
            for (Budget budget : budgetRepository.findCovering(event.memberId(), event.categoryIds(), day)) {
                budgets.putIfAbsent(budget.getId(), budget);
            }
        }
        if (budgets.isEmpty()) {
            return;
        }
        Member member = validationService.getMemberById(event.memberId());
        for (BudgetResponse budget : budgetService.evaluate(event.memberId(), new ArrayList<>(budgets.values()))) {
            int level = budget.getPercent() >= FULL_PERCENT ? FULL_PERCENT
                    : budget.getPercent() >= ALERT_PERCENT ? ALERT_PERCENT : 0;
            if (level == 0) {
                continue;
            }
            notificationService.notify(member, NotificationType.BUDGET_ALERT, "BUDGET:" + budget.getId() + ":" + level,
                    NotificationMessages.budgetAlert(member.getLanguage(), budget.getCategoryName(), budget.getPercent()),
                    payload(budget));
        }
    }

    private static Map<String, String> payload(BudgetResponse budget) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("route", "budget");
        payload.put("budgetId", budget.getId());
        payload.put("month", budget.getPeriodStart().toString().substring(0, 7));
        return payload;
    }
}
