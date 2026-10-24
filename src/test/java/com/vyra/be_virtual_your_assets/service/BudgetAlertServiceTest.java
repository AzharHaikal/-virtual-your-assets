package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.entity.Budget;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.event.TransactionChangedEvent;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAlertServiceTest {

    private static final String MEMBER = "member-1";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

    @Mock private BudgetRepository budgetRepository;
    @Mock private BudgetService budgetService;
    @Mock private ValidationService validationService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private BudgetAlertService service;

    private final Member member = new Member();

    @BeforeEach
    void setUp() {
        member.setId(MEMBER);
        member.setLanguage(AppLanguage.EN);
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(budgetRepository.findCovering(eq(MEMBER), any(), eq(DAY))).thenReturn(List.of(budget("b-1")));
    }

    @Test
    void reaching80Percent_notifiesOncePerBudgetAndLevel() {
        when(budgetService.evaluate(eq(MEMBER), anyList())).thenReturn(List.of(evaluated("b-1", 85)));

        service.onTransactionChanged(event());

        verify(notificationService).notify(eq(member), eq(NotificationType.BUDGET_ALERT), eq("BUDGET:b-1:80"), any(), anyMap());
    }

    @Test
    void reaching100Percent_usesTheFullLevel() {
        when(budgetService.evaluate(eq(MEMBER), anyList())).thenReturn(List.of(evaluated("b-1", 130)));

        service.onTransactionChanged(event());

        verify(notificationService).notify(eq(member), eq(NotificationType.BUDGET_ALERT), eq("BUDGET:b-1:100"), any(), anyMap());
    }

    @Test
    void below80Percent_orNoBudget_notifiesNothing() {
        when(budgetService.evaluate(eq(MEMBER), anyList())).thenReturn(List.of(evaluated("b-1", 79)));
        service.onTransactionChanged(event());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());

        when(budgetRepository.findCovering(eq(MEMBER), any(), eq(DAY))).thenReturn(List.of());
        service.onTransactionChanged(event());
        verify(budgetService, org.mockito.Mockito.times(1)).evaluate(any(), anyList());
    }

    @Test
    void aFailure_neverReachesTheTransaction() {
        when(budgetService.evaluate(eq(MEMBER), anyList())).thenThrow(new IllegalStateException("db down"));

        service.onTransactionChanged(event()); // no exception

        verifyNoInteractions(notificationService);
    }

    private static TransactionChangedEvent event() {
        return new TransactionChangedEvent(MEMBER, Set.of("default-FOOD_AND_BEVERAGE"), Set.of(DAY));
    }

    private static Budget budget(String id) {
        Budget budget = new Budget();
        budget.setId(id);
        return budget;
    }

    private static BudgetResponse evaluated(String id, int percent) {
        BudgetResponse response = new BudgetResponse();
        response.setId(id);
        response.setCategoryName("Food");
        response.setPercent(percent);
        response.setPeriodStart(LocalDate.of(2026, 10, 1));
        return response;
    }
}
