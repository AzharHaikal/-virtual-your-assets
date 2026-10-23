package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.finance.FinanceSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.habit.HabitResponse;
import com.vyra.be_virtual_your_assets.dto.home.HomeActivityResponse;
import com.vyra.be_virtual_your_assets.dto.home.HomeSummaryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The member's home screen in one call: this month's money, today's mood, today's habits, the top
 * active goals and the latest activity of every module. Reuses the module services so the numbers
 * are the same as on each module's own screen.
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    static final int ACTIVITY_LIMIT = 8;
    static final int TOP_GOALS = 3;
    static final int TOP_CATEGORIES = 3;
    /** Mood average and habit/mood activity look this many days back (today included). */
    static final int RECENT_DAYS = 7;

    private final ValidationService validationService;
    private final TimeService timeService;
    private final WalletService walletService;
    private final FinanceService financeService;
    private final TransactionService transactionService;
    private final MoodService moodService;
    private final HabitService habitService;
    private final GoalService goalService;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final GoalProgressLogRepository progressLogRepository;

    @Transactional(readOnly = true)
    public BaseResponse<HomeSummaryResponse> getSummary(String memberId) {
        Member member = validationService.getMemberById(memberId);
        LocalDate today = timeService.today(timeService.zoneOf(member));
        LocalDate weekStart = today.minusDays(RECENT_DAYS - 1L);

        List<MoodEntryResponse> moods = moodService.getEntries(memberId, weekStart, today).getData();
        List<TransactionHistoryResponse> transactions = transactionService.getTopTransactionHistory(memberId).getData();

        HomeSummaryResponse response = new HomeSummaryResponse();
        response.setFirstName(member.getFirstName());
        response.setToday(today);
        response.setFinance(finance(member));
        response.setMood(mood(moods, today));
        response.setHabits(habits(memberId));
        response.setGoals(goals(memberId));
        response.setRecentActivity(recentActivity(memberId, transactions, moods, weekStart, today));
        return new BaseResponse<>(ErrorConstant.HOME_SUMMARY_SUCCESS.getCode(),
                ErrorConstant.HOME_SUMMARY_SUCCESS.getMessage(), response);
    }

    // =========================================================================
    // Cards
    // =========================================================================

    private HomeSummaryResponse.Finance finance(Member member) {
        FinanceSummaryResponse month = financeService.getSummary(member.getId(), null, null).getData();
        HomeSummaryResponse.Finance finance = new HomeSummaryResponse.Finance();
        finance.setBalance(balance(member));
        finance.setFrom(month.getFrom());
        finance.setTo(month.getTo());
        finance.setTotalIncome(month.getTotalIncome());
        finance.setTotalExpense(month.getTotalExpense());
        finance.setNet(month.getNet());
        finance.setPreviousExpense(month.getPreviousExpense());
        finance.setHasTransactions(!month.getIncomeByCategory().isEmpty() || !month.getExpenseByCategory().isEmpty());
        finance.setTopExpenseCategories(month.getExpenseByCategory().stream().limit(TOP_CATEGORIES).toList());
        return finance;
    }

    /** null for a member without a wallet. */
    private BigDecimal balance(Member member) {
        try {
            return walletService.getMemberWallet(member.getPhoneNumber()).getData().getBalance();
        } catch (BusinessException e) {
            return null;
        }
    }

    private static HomeSummaryResponse.Mood mood(List<MoodEntryResponse> moods, LocalDate today) {
        HomeSummaryResponse.Mood mood = new HomeSummaryResponse.Mood();
        moods.stream().filter(m -> today.equals(m.getLocalDate())).findFirst().ifPresent(entry -> {
            // The home card shows the mood only; the journal stays on the Mood screen.
            MoodEntryResponse withoutJournal = new MoodEntryResponse();
            withoutJournal.setId(entry.getId());
            withoutJournal.setLocalDate(entry.getLocalDate());
            withoutJournal.setMoodScore(entry.getMoodScore());
            withoutJournal.setTags(entry.getTags());
            withoutJournal.setCheckedInAt(entry.getCheckedInAt());
            mood.setToday(withoutJournal);
        });
        mood.setWeekEntries(moods.size());
        if (!moods.isEmpty()) {
            int sum = moods.stream().mapToInt(MoodEntryResponse::getMoodScore).sum();
            mood.setWeekAverage(BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(moods.size()), 1, RoundingMode.HALF_UP));
        }
        return mood;
    }

    private HomeSummaryResponse.Habits habits(String memberId) {
        List<HabitResponse> today = habitService.getToday(memberId).getData();
        HomeSummaryResponse.Habits habits = new HomeSummaryResponse.Habits();
        habits.setToday(today);
        habits.setTotalToday(today.size());
        habits.setDoneToday((int) today.stream().filter(HabitResponse::isDoneToday).count());
        habits.setHasHabits(!today.isEmpty()
                || habitRepository.findAllOfMember(memberId).stream().anyMatch(h -> !h.isArchived()));
        return habits;
    }

    private HomeSummaryResponse.Goals goals(String memberId) {
        List<GoalResponse> active = goalService.getGoals(memberId, List.of(GoalStatus.ACTIVE)).getData();
        HomeSummaryResponse.Goals goals = new HomeSummaryResponse.Goals();
        goals.setActiveCount(active.size());
        goals.setTop(active.stream().limit(TOP_GOALS).toList());
        return goals;
    }

    // =========================================================================
    // Activity feed
    // =========================================================================

    private List<HomeActivityResponse> recentActivity(String memberId, List<TransactionHistoryResponse> transactions,
                                                      List<MoodEntryResponse> moods, LocalDate from, LocalDate to) {
        List<HomeActivityResponse> items = new ArrayList<>();
        transactions.forEach(t -> items.add(transaction(t)));
        moods.forEach(m -> items.add(mood(m)));

        List<HabitCompletion> completions = completionRepository.findRange(memberId, from, to);
        if (!completions.isEmpty()) {
            Map<String, Habit> habits = habitRepository.findAllOfMember(memberId).stream()
                    .collect(Collectors.toMap(Habit::getId, Function.identity()));
            completions.stream()
                    .filter(c -> habits.containsKey(c.getHabitId()))
                    .forEach(c -> items.add(habit(c, habits.get(c.getHabitId()))));
        }
        for (Object[] row : progressLogRepository.findLatestOfMember(memberId)) {
            items.add(goal((GoalProgressLog) row[0], (Goal) row[1]));
        }

        return items.stream()
                .filter(i -> i.getOccurredAt() != null)
                .sorted(Comparator.comparing(HomeActivityResponse::getOccurredAt).reversed())
                .limit(ACTIVITY_LIMIT)
                .toList();
    }

    private static HomeActivityResponse transaction(TransactionHistoryResponse t) {
        HomeActivityResponse item = activity("TRANSACTION", t.getTransactionId(), t.getTransactionDate());
        item.setTitle(Objects.requireNonNullElse(t.getCategoryName(), t.getCategory()));
        item.setIcon(t.getCategoryIcon());
        item.setColor(t.getCategoryColor());
        item.setTransactionType(t.getType() != null ? t.getType().name() : null);
        item.setAmount(t.getAmount() != null ? t.getAmount().abs() : null);
        return item;
    }

    /** No journal text: the feed only says that the member checked in. */
    private static HomeActivityResponse mood(MoodEntryResponse m) {
        HomeActivityResponse item = activity("MOOD", m.getId(), m.getCheckedInAt());
        item.setMoodScore(m.getMoodScore());
        return item;
    }

    private static HomeActivityResponse habit(HabitCompletion c, Habit habit) {
        HomeActivityResponse item = activity("HABIT", habit.getId(), c.getCompletedAt());
        item.setTitle(habit.getName());
        item.setIcon(habit.getIcon());
        item.setColor(habit.getColor());
        item.setCount(c.getCount());
        item.setTargetCount(habit.getTargetCount());
        return item;
    }

    private static HomeActivityResponse goal(GoalProgressLog log, Goal goal) {
        HomeActivityResponse item = activity("GOAL", goal.getId(), log.getCreatedAt());
        item.setTitle(goal.getTitle());
        item.setIcon(goal.getIcon());
        item.setColor(goal.getColor());
        item.setAmount(log.getDelta());
        item.setUnit(log.getDelta() != null ? goal.getUnit() : null);
        item.setHasNote(log.getNote() != null);
        return item;
    }

    private static HomeActivityResponse activity(String type, String refId, LocalDateTime occurredAt) {
        HomeActivityResponse item = new HomeActivityResponse();
        item.setType(type);
        item.setRefId(refId);
        item.setOccurredAt(occurredAt);
        return item;
    }
}
