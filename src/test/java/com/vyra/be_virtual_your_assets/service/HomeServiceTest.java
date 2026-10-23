package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.finance.CategoryAmountResponse;
import com.vyra.be_virtual_your_assets.dto.finance.FinanceSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.home.HomeActivityResponse;
import com.vyra.be_virtual_your_assets.dto.home.HomeSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-22 17:30 UTC = 2026-10-23 00:30 in Jakarta. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 17, 30);

    @Mock private ValidationService validationService;
    @Mock private WalletService walletService;
    @Mock private FinanceService financeService;
    @Mock private TransactionService transactionService;
    @Mock private MoodService moodService;
    @Mock private HabitService habitService;
    @Mock private GoalService goalService;
    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private GoalProgressLogRepository progressLogRepository;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        homeService = new HomeService(validationService, timeService, walletService, financeService,
                transactionService, moodService, habitService, goalService, habitRepository, completionRepository,
                progressLogRepository);
        Member member = new Member();
        member.setId(MEMBER);
        member.setFirstName("Azhar");
        member.setPhoneNumber("628123");
        member.setTimezone("Asia/Jakarta");
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
    }

    @Test
    void newMember_everyCardIsEmpty_notZeroData() {
        stub(finance(List.of(), List.of()), List.of(), List.of(), List.of(), List.of());
        when(walletService.getMemberWallet("628123")).thenThrow(new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
        when(habitRepository.findAllOfMember(MEMBER)).thenReturn(List.of());

        HomeSummaryResponse result = homeService.getSummary(MEMBER).getData();

        assertThat(result.getFirstName()).isEqualTo("Azhar");
        assertThat(result.getToday()).isEqualTo(TODAY);
        assertThat(result.getFinance().getBalance()).isNull();
        assertThat(result.getFinance().isHasTransactions()).isFalse();
        assertThat(result.getMood().getToday()).isNull();
        assertThat(result.getMood().getWeekAverage()).isNull();
        assertThat(result.getHabits().isHasHabits()).isFalse();
        assertThat(result.getHabits().getTotalToday()).isZero();
        assertThat(result.getGoals().getActiveCount()).isZero();
        assertThat(result.getRecentActivity()).isEmpty();
    }

    @Test
    void cards_summarizeEachModule() {
        List<CategoryAmountResponse> expense = IntStream.range(0, 5).mapToObj(i -> category("c" + i)).toList();
        HabitResponse done = habit("h-1", true);
        HabitResponse open = habit("h-2", false);
        List<GoalResponse> goals = IntStream.range(0, 4).mapToObj(i -> goal("g" + i)).toList();
        MoodEntryResponse todayMood = mood(TODAY, 4, NOW);
        todayMood.setNote("jurnal");
        stub(finance(List.of(), expense), List.of(todayMood, mood(TODAY.minusDays(1), 3, NOW.minusDays(1))),
                List.of(done, open), goals, List.of());
        GetMemberWalletResponse wallet = new GetMemberWalletResponse();
        wallet.setBalance(new BigDecimal("1500000"));
        when(walletService.getMemberWallet("628123")).thenReturn(new BaseResponse<>("ok", "ok", wallet));

        HomeSummaryResponse result = homeService.getSummary(MEMBER).getData();

        assertThat(result.getFinance().getBalance()).isEqualByComparingTo("1500000");
        assertThat(result.getFinance().isHasTransactions()).isTrue();
        assertThat(result.getFinance().getTopExpenseCategories()).extracting(CategoryAmountResponse::getCategoryId)
                .containsExactly("c0", "c1", "c2");
        assertThat(result.getMood().getToday().getMoodScore()).isEqualTo(4);
        assertThat(result.getMood().getToday().getNote()).isNull();
        assertThat(result.getMood().getWeekAverage()).isEqualByComparingTo("3.5");
        assertThat(result.getHabits().getDoneToday()).isEqualTo(1);
        assertThat(result.getHabits().getTotalToday()).isEqualTo(2);
        assertThat(result.getHabits().isHasHabits()).isTrue();
        assertThat(result.getGoals().getActiveCount()).isEqualTo(4);
        assertThat(result.getGoals().getTop()).extracting(GoalResponse::getId).containsExactly("g0", "g1", "g2");
    }

    @Test
    void activity_mergesModulesNewestFirst_withoutJournalOrDeletedHabits() {
        TransactionHistoryResponse tx = new TransactionHistoryResponse();
        tx.setTransactionId("t-1");
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(new BigDecimal("-50000"));
        tx.setCategoryName("Makan");
        tx.setTransactionDate(NOW.minusHours(1));
        MoodEntryResponse mood = mood(TODAY, 5, NOW.minusMinutes(10));
        mood.setNote("rahasia");
        Habit habit = new Habit();
        habit.setId("h-1");
        habit.setName("Minum air");
        habit.setTargetCount(8);
        HabitCompletion completion = completion("h-1", NOW.minusMinutes(5));
        HabitCompletion ofDeletedHabit = completion("h-gone", NOW);
        Goal goal = new Goal();
        goal.setId("g-1");
        goal.setTitle("Dana darurat");
        goal.setUnit("IDR");
        GoalProgressLog log = new GoalProgressLog();
        log.setDelta(new BigDecimal("100000"));
        log.setCreatedAt(NOW.minusMinutes(30));

        stub(finance(List.of(), List.of()), List.of(mood), List.of(), List.of(), List.of(tx));
        when(walletService.getMemberWallet("628123")).thenThrow(new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
        when(habitRepository.findAllOfMember(MEMBER)).thenReturn(List.of(habit));
        when(completionRepository.findRange(MEMBER, TODAY.minusDays(6), TODAY)).thenReturn(List.of(completion, ofDeletedHabit));
        List<Object[]> logs = new ArrayList<>();
        logs.add(new Object[]{log, goal});
        when(progressLogRepository.findLatestOfMember(MEMBER)).thenReturn(logs);

        List<HomeActivityResponse> feed = homeService.getSummary(MEMBER).getData().getRecentActivity();

        assertThat(feed).extracting(HomeActivityResponse::getType).containsExactly("HABIT", "MOOD", "GOAL", "TRANSACTION");
        assertThat(feed.get(0).getCount()).isEqualTo(3);
        assertThat(feed.get(0).getTargetCount()).isEqualTo(8);
        assertThat(feed.get(1).getMoodScore()).isEqualTo(5);
        assertThat(feed.get(1).toString()).doesNotContain("rahasia");
        assertThat(feed.get(2).getUnit()).isEqualTo("IDR");
        assertThat(feed.get(3).getAmount()).isEqualByComparingTo("50000");
        assertThat(feed.get(3).getTransactionType()).isEqualTo("EXPENSE");
    }

    // ------------------------------------------------------------------
    private void stub(FinanceSummaryResponse finance, List<MoodEntryResponse> moods, List<HabitResponse> habits,
                      List<GoalResponse> goals, List<TransactionHistoryResponse> transactions) {
        when(financeService.getSummary(MEMBER, null, null)).thenReturn(new BaseResponse<>("ok", "ok", finance));
        when(moodService.getEntries(MEMBER, TODAY.minusDays(6), TODAY)).thenReturn(new BaseResponse<>("ok", "ok", moods));
        when(habitService.getToday(MEMBER)).thenReturn(new BaseResponse<>("ok", "ok", habits));
        when(goalService.getGoals(MEMBER, List.of(GoalStatus.ACTIVE))).thenReturn(new BaseResponse<>("ok", "ok", goals));
        when(transactionService.getTopTransactionHistory(MEMBER)).thenReturn(new BaseResponse<>("ok", "ok", transactions));
    }

    private static FinanceSummaryResponse finance(List<CategoryAmountResponse> income, List<CategoryAmountResponse> expense) {
        FinanceSummaryResponse finance = new FinanceSummaryResponse();
        finance.setFrom(TODAY.withDayOfMonth(1));
        finance.setTo(TODAY.withDayOfMonth(31));
        finance.setTotalIncome(BigDecimal.ZERO);
        finance.setTotalExpense(BigDecimal.ZERO);
        finance.setNet(BigDecimal.ZERO);
        finance.setIncomeByCategory(income);
        finance.setExpenseByCategory(expense);
        return finance;
    }

    private static CategoryAmountResponse category(String id) {
        CategoryAmountResponse category = new CategoryAmountResponse();
        category.setCategoryId(id);
        category.setAmount(BigDecimal.TEN);
        return category;
    }

    private static MoodEntryResponse mood(LocalDate day, int score, LocalDateTime at) {
        MoodEntryResponse mood = new MoodEntryResponse();
        mood.setId("mood-" + day);
        mood.setLocalDate(day);
        mood.setMoodScore(score);
        mood.setCheckedInAt(at);
        return mood;
    }

    private static HabitResponse habit(String id, boolean done) {
        HabitResponse habit = new HabitResponse();
        habit.setId(id);
        habit.setDoneToday(done);
        return habit;
    }

    private static GoalResponse goal(String id) {
        GoalResponse goal = new GoalResponse();
        goal.setId(id);
        return goal;
    }

    private static HabitCompletion completion(String habitId, LocalDateTime at) {
        HabitCompletion completion = new HabitCompletion();
        completion.setHabitId(habitId);
        completion.setMemberId(MEMBER);
        completion.setLocalDate(TODAY);
        completion.setCount(3);
        completion.setCompletedAt(at);
        return completion;
    }
}
