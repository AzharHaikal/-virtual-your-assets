package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.AppModule;
import com.vyra.be_virtual_your_assets.constant.BudgetLevel;
import com.vyra.be_virtual_your_assets.constant.DashboardCard;
import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.constant.TrendPeriod;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.account.AccountResponse;
import com.vyra.be_virtual_your_assets.dto.account.DashboardCardSetting;
import com.vyra.be_virtual_your_assets.dto.account.UpdatePreferencesRequest;
import com.vyra.be_virtual_your_assets.dto.budget.BudgetResponse;
import com.vyra.be_virtual_your_assets.dto.budget.CreateBudgetRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferenceItem;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferencesResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UpdateNotificationPreferencesRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.scheduler.GoalDueJob;
import com.vyra.be_virtual_your_assets.scheduler.HabitReminderJob;
import com.vyra.be_virtual_your_assets.service.AccountService;
import com.vyra.be_virtual_your_assets.service.BudgetService;
import com.vyra.be_virtual_your_assets.service.DataExportService;
import com.vyra.be_virtual_your_assets.service.FinanceService;
import com.vyra.be_virtual_your_assets.service.GoalService;
import com.vyra.be_virtual_your_assets.service.HabitService;
import com.vyra.be_virtual_your_assets.service.InsightService;
import com.vyra.be_virtual_your_assets.service.MoodService;
import com.vyra.be_virtual_your_assets.service.NotificationMessages;
import com.vyra.be_virtual_your_assets.service.NotificationPreferenceService;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 2 modules on a real PostgreSQL: the new migrations, jsonb columns, native upserts and the
 * cross-schema queries of the jobs. The scheduler is off so only these calls create notifications.
 * Opt-in, see {@link SchemaMigrationIT} for how to run it ({@code -Dvyra.it.db=true}).
 */
@SpringBootTest(properties = "vyra.scheduler.enabled=false")
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class Phase2ModulesIT {

    @Autowired private MemberRepository memberRepository;
    @Autowired private WalletService walletService;
    @Autowired private TimeService timeService;
    @Autowired private TransactionService transactionService;
    @Autowired private BudgetService budgetService;
    @Autowired private FinanceService financeService;
    @Autowired private GoalService goalService;
    @Autowired private HabitService habitService;
    @Autowired private MoodService moodService;
    @Autowired private InsightService insightService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationPreferenceService preferenceService;
    @Autowired private AccountService accountService;
    @Autowired private DataExportService dataExportService;
    @Autowired private HabitReminderJob habitReminderJob;
    @Autowired private GoalDueJob goalDueJob;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void budget_followsTheTransactions_andAlertsOnceAt80Percent() {
        Member member = ItMembers.create(memberRepository, walletService);
        CreateBudgetRequest budget = new CreateBudgetRequest();
        budget.setCategoryId("default-FOOD_AND_BEVERAGE");
        budget.setAmount(BigDecimal.valueOf(100_000));
        String budgetId = budgetService.createBudget(member.getId(), budget).getData().getId();

        String txId = transactionService.createTransaction(member.getId(), expense(85_000)).getData().getTransactionId();
        transactionService.createTransaction(member.getId(), expense(1_000));

        BudgetResponse spent = budgetService.getBudgets(member.getId(), null).getData().getItems().get(0);
        assertThat(spent.getSpent()).isEqualByComparingTo("86000");
        assertThat(spent.getLevel()).isEqualTo(BudgetLevel.WARNING);
        List<NotificationResponse> inbox = notificationService.getNotifications(member.getId(), false, 0, 20)
                .getData().getContent();
        assertThat(inbox).singleElement().satisfies(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.BUDGET_ALERT);
            assertThat(n.getPayload()).containsEntry("budgetId", budgetId);
        });

        transactionService.deleteOwnTransaction(member.getId(), txId);
        assertThat(budgetService.getBudgets(member.getId(), null).getData().getItems().get(0).getSpent())
                .isEqualByComparingTo("1000");
    }

    @Test
    void notification_dedupAndPreferences_onTheRealTables() {
        Member member = ItMembers.create(memberRepository, walletService);
        String key = "SYSTEM:it:" + UUID.randomUUID();
        NotificationMessages.Message message = NotificationMessages.habitReminder(member.getLanguage(), "Minum air");

        assertThat(notificationService.notify(member, NotificationType.SYSTEM, key, message, Map.of("route", "home"))).isTrue();
        assertThat(notificationService.notify(member, NotificationType.SYSTEM, key, message, Map.of("route", "home"))).isFalse();
        assertThat(notificationService.getUnreadCount(member.getId()).getData().getCount()).isEqualTo(1);
        assertThat(notificationService.markAllRead(member.getId()).getData().getUpdated()).isEqualTo(1);

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        request.setQuietStart(LocalTime.of(22, 0));
        request.setQuietEnd(LocalTime.of(7, 0));
        request.setItems(List.of(new NotificationPreferenceItem(NotificationType.GOAL_DUE, NotificationChannel.PUSH, false)));
        preferenceService.updatePreferences(member.getId(), request);
        preferenceService.updatePreferences(member.getId(), request); // second save updates the same rows

        NotificationPreferencesResponse saved = preferenceService.getPreferences(member.getId()).getData();
        assertThat(saved.getQuietStart()).isEqualTo(LocalTime.of(22, 0));
        assertThat(saved.getItems()).contains(
                new NotificationPreferenceItem(NotificationType.GOAL_DUE, NotificationChannel.PUSH, false));
    }

    @Test
    void habitReminder_andGoalDue_jobsFindTheirRows_once() {
        Member member = ItMembers.create(memberRepository, walletService);
        LocalDateTime now = timeService.nowUtc().withSecond(0).withNano(0);
        LocalTime localNow = now.atOffset(ZoneOffset.UTC).atZoneSameInstant(timeService.zoneOf(member)).toLocalTime();

        CreateHabitRequest habit = new CreateHabitRequest();
        habit.setName("Minum air");
        habit.setCategory(HabitCategory.HEALTH);
        habit.setIcon("local_drink");
        habit.setColor("#22C55E");
        habit.setScheduleType(HabitScheduleType.DAILY);
        habit.setReminderEnabled(true);
        habit.setReminderTime(localNow);
        HabitResponse created = habitService.createHabit(member.getId(), habit).getData();
        assertThat(created.getReminderTime()).isEqualTo(localNow);
        assertThat(created.getCurrentStreak()).isZero();

        // Unless it is just after midnight (then the window only covers today), the reminder is due now.
        if (localNow.isAfter(LocalTime.of(0, 5))) {
            assertThat(habitReminderJob.run(now)).isGreaterThanOrEqualTo(1);
            assertThat(habitReminderJob.run(now)).isZero();
        }

        LocalDate localToday = timeService.today(timeService.zoneOf(member));
        CreateGoalRequest goal = new CreateGoalRequest();
        goal.setTitle("Laptop");
        goal.setCategory(GoalCategory.FINANCE);
        goal.setIcon("savings");
        goal.setColor("#3B82F6");
        goal.setProgressType(GoalProgressType.VALUE);
        goal.setTargetValue(BigDecimal.valueOf(15_000_000));
        goal.setLinkedToFinance(true);
        goal.setDueDate(localToday.plusDays(3));
        goalService.createGoal(member.getId(), goal);
        assertThat(financeService.getSummary(member.getId(), null, null).getData().getSavings().getActiveGoals()).isEqualTo(1);

        // 09:00 local on the member's day.
        LocalDateTime nineLocal = localToday.atTime(9, 0).atZone(timeService.zoneOf(member))
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        assertThat(goalDueJob.run(nineLocal)).isGreaterThanOrEqualTo(1);
        assertThat(goalDueJob.run(nineLocal)).isZero();
    }

    @Test
    void trendsCalendarAndInsight_runTheirQueries() {
        Member member = ItMembers.create(memberRepository, walletService);
        assertThat(moodService.getTrend(member.getId(), TrendPeriod.MONTH, null).getData().getPoints()).isNotEmpty();
        assertThat(moodService.getTagsSummary(member.getId(), null, null).getData().getEntries()).isZero();
        assertThat(insightService.getWeekly(member.getId(), null).getData().getHabits().getPercent()).isNull();
        assertThat(habitService.getHabits(member.getId(), null, false).getData()).isEmpty();
        assertThrows(NotFoundException.class,
                () -> habitService.getCalendar(member.getId(), UUID.randomUUID().toString(), YearMonth.now()));
    }

    @Test
    void dashboardLayout_isStoredAsJson_andDataExportIsBuilt() {
        Member member = ItMembers.create(memberRepository, walletService);
        member.setPin(passwordEncoder.encode("123456"));
        memberRepository.saveAndFlush(member);

        UpdatePreferencesRequest preferences = new UpdatePreferencesRequest();
        preferences.setEnabledModules(List.of(AppModule.FINANCE));
        preferences.setDashboardLayout(List.of(new DashboardCardSetting(DashboardCard.GOALS, true)));
        accountService.updatePreferences(member.getId(), preferences);
        AccountResponse account = accountService.getAccount(member.getId()).getData();
        assertThat(account.getEnabledModules()).containsExactly(AppModule.FINANCE);
        assertThat(account.getDashboardLayout().get(0)).isEqualTo(new DashboardCardSetting(DashboardCard.GOALS, true));

        String exportId = dataExportService.request(member.getId(), "123456").getData().getId();
        dataExportService.process(exportId); // the e-mail may fail in tests; the export stays downloadable
        assertThat(dataExportService.get(member.getId(), exportId).getData().getStatus()).isEqualTo(DataExportStatus.READY);
        assertThat(dataExportService.download(member.getId(), exportId).content()).isNotEmpty();
    }

    private static CreateTransactionRequest expense(long amount) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(TransactionType.EXPENSE);
        request.setCategory(TransactionCategory.FOOD_AND_BEVERAGE);
        request.setAmount(BigDecimal.valueOf(amount));
        request.setTransactionDesc("Makan");
        return request;
    }
}
