package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.GoalStatus;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.dto.insight.AdvancedInsightResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvancedInsightServiceTest {

    /** 2026-10-25 10:00 WIB. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private MoodEntryRepository moodEntryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private GoalProgressLogRepository progressLogRepository;
    @Mock private ValidationService validationService;

    private AdvancedInsightService service;

    @BeforeEach
    void setUp() {
        service = new AdvancedInsightService(habitRepository, completionRepository, moodEntryRepository,
                transactionRepository, goalRepository, progressLogRepository, validationService, new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void habitMood_comparesDoneAndMissedDays_onlyWithEnoughData() {
        LocalDate from = TODAY.minusDays(89);
        Habit exercise = habit("h-1", "Olahraga");
        Habit reading = habit("h-2", "Membaca");
        when(habitRepository.findAllOfMember("m-1")).thenReturn(List.of(exercise, reading));
        List<MoodEntry> moods = new ArrayList<>();
        List<Object[]> done = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            LocalDate day = TODAY.minusDays(i);
            boolean exercised = i % 2 == 0;
            moods.add(mood(day, exercised ? 4 : 2));
            if (exercised) {
                done.add(new Object[]{"h-1", day});
            }
        }
        done.add(new Object[]{"h-2", TODAY.minusDays(1)});
        when(moodEntryRepository.findRange("m-1", from, TODAY)).thenReturn(moods);
        when(completionRepository.findDoneDays("m-1", from, TODAY)).thenReturn(done);

        List<AdvancedInsightResponse.HabitMood> result = service.habitMood("m-1", from, TODAY);

        assertThat(result).singleElement().satisfies(h -> {
            assertThat(h.getName()).isEqualTo("Olahraga");
            assertThat(h.getDoneDays()).isEqualTo(6);
            assertThat(h.getMissedDays()).isEqualTo(6);
            assertThat(h.getMoodWhenDone()).isEqualByComparingTo("4");
            assertThat(h.getMoodWhenMissed()).isEqualByComparingTo("2");
            assertThat(h.getDifference()).isEqualByComparingTo("2");
        });
    }

    @Test
    void months_last12OldestFirst_withIncomeExpenseAndNet() {
        when(transactionRepository.sumByCategory(eq("m-1"), any(), any())).thenReturn(List.of());
        when(transactionRepository.sumByCategory("m-1", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.<Object[]>of(new Object[]{"c-1", "INCOME", new BigDecimal("5000000")},
                        new Object[]{"c-2", "EXPENSE", new BigDecimal("-1200000")}));

        List<AdvancedInsightResponse.Month> result = service.months("m-1", YearMonth.of(2026, 10));

        assertThat(result).hasSize(12);
        assertThat(result.get(0).getMonth()).isEqualTo("2025-11");
        AdvancedInsightResponse.Month october = result.get(11);
        assertThat(october.getMonth()).isEqualTo("2026-10");
        assertThat(october.getExpense()).isEqualByComparingTo("1200000");
        assertThat(october.getNet()).isEqualByComparingTo("3800000");
    }

    @Test
    void goals_projectWithThePaceOfTheLast90Days() {
        Goal goal = new Goal();
        goal.setId("g-1");
        goal.setTitle("Dana darurat");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setProgressType(GoalProgressType.VALUE);
        goal.setTargetValue(new BigDecimal("10000000"));
        goal.setCurrentValue(new BigDecimal("4000000"));
        goal.setDueDate(LocalDate.of(2027, 1, 31));
        goal.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        Goal milestones = new Goal();
        milestones.setId("g-2");
        milestones.setStatus(GoalStatus.ACTIVE);
        milestones.setProgressType(GoalProgressType.MILESTONE);
        when(goalRepository.findAllOfMember("m-1")).thenReturn(List.of(goal, milestones));
        GoalProgressLog old = log("g-1", "1000000", LocalDateTime.of(2026, 6, 1, 0, 0));
        GoalProgressLog recent = log("g-1", "3000000", LocalDateTime.of(2026, 9, 1, 0, 0));
        when(progressLogRepository.findByGoalIdInOrderByCreatedAtAsc(List.of("g-1"))).thenReturn(List.of(old, recent));

        List<AdvancedInsightResponse.GoalProjection> result = service.goals("m-1", TODAY);

        assertThat(result).singleElement().satisfies(p -> {
            assertThat(p.getPacePerMonth()).isEqualByComparingTo("1000000");
            assertThat(p.getProjectedDate()).isEqualTo(TODAY.plusDays(180));
            assertThat(p.getOnTrack()).isFalse();
        });
    }

    @Test
    void get_wrapsAllSections() {
        Member member = new Member();
        member.setId("m-1");
        member.setTimezone("Asia/Jakarta");
        when(validationService.getMemberById("m-1")).thenReturn(member);
        when(moodEntryRepository.findRange("m-1", TODAY.minusDays(89), TODAY)).thenReturn(List.of());
        when(transactionRepository.sumByCategory(eq("m-1"), any(), any())).thenReturn(List.of());
        when(goalRepository.findAllOfMember("m-1")).thenReturn(List.of());

        AdvancedInsightResponse result = service.get("m-1").getData();

        assertThat(result.getFrom()).isEqualTo(TODAY.minusDays(89));
        assertThat(result.getTo()).isEqualTo(TODAY);
        assertThat(result.getHabitMood()).isEmpty();
        assertThat(result.getMonths()).hasSize(12);
        assertThat(result.getGoals()).isEmpty();
    }

    private static Habit habit(String id, String name) {
        Habit habit = new Habit();
        habit.setId(id);
        habit.setName(name);
        habit.setCategory(HabitCategory.HEALTH);
        habit.setStartDate(TODAY.minusDays(60));
        return habit;
    }

    private static MoodEntry mood(LocalDate day, int score) {
        MoodEntry entry = new MoodEntry();
        entry.setLocalDate(day);
        entry.setMoodScore((short) score);
        return entry;
    }

    private static GoalProgressLog log(String goalId, String delta, LocalDateTime at) {
        GoalProgressLog log = new GoalProgressLog();
        log.setGoalId(goalId);
        log.setDelta(new BigDecimal(delta));
        log.setCreatedAt(at);
        return log;
    }
}
