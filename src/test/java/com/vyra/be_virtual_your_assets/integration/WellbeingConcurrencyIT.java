package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressRequest;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.HabitCompletionRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryRequest;
import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.service.GoalService;
import com.vyra.be_virtual_your_assets.service.HabitService;
import com.vyra.be_virtual_your_assets.service.MoodService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The "one per day" rules hold under parallel requests on a real PostgreSQL: one mood entry per
 * member and day, one habit completion per habit and day, and goal progress that adds up.
 * Opt-in, see {@link SchemaMigrationIT} for how to run it ({@code -Dvyra.it.db=true}).
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class WellbeingConcurrencyIT {

    private static final int PARALLEL = 4;

    @Autowired private MemberRepository memberRepository;
    @Autowired private WalletService walletService;
    @Autowired private TimeService timeService;
    @Autowired private MoodService moodService;
    @Autowired private MoodEntryRepository moodEntryRepository;
    @Autowired private HabitService habitService;
    @Autowired private HabitCompletionRepository completionRepository;
    @Autowired private GoalService goalService;

    @Test
    void parallelCheckInsOnTheSameDay_leaveOneMoodEntry() throws Exception {
        Member member = ItMembers.create(memberRepository, walletService);
        LocalDate today = timeService.today(timeService.zoneOf(member));

        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 1; i <= PARALLEL; i++) {
            int score = i;
            tasks.add(() -> {
                MoodEntryRequest request = new MoodEntryRequest();
                request.setMoodScore(score);
                return moodService.saveEntry(member.getId(), today, request);
            });
        }

        assertThat(ItMembers.runTogether(tasks)).containsOnlyNulls();
        List<MoodEntry> entries = moodEntryRepository.findRange(member.getId(), today, today);
        assertThat(entries).hasSize(1);
        assertThat((int) entries.get(0).getMoodScore()).isBetween(1, PARALLEL);
    }

    @Test
    void parallelTicksOnTheSameDay_leaveOneHabitCompletion() throws Exception {
        Member member = ItMembers.create(memberRepository, walletService);
        LocalDate today = timeService.today(timeService.zoneOf(member));
        CreateHabitRequest habit = new CreateHabitRequest();
        habit.setName("Minum air");
        habit.setCategory(HabitCategory.HEALTH);
        habit.setIcon("local_drink");
        habit.setColor("#3B82F6");
        habit.setTargetCount(8);
        habit.setScheduleType(HabitScheduleType.DAILY);
        String habitId = habitService.createHabit(member.getId(), habit).getData().getId();

        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 1; i <= PARALLEL; i++) {
            int count = i;
            tasks.add(() -> {
                HabitCompletionRequest request = new HabitCompletionRequest();
                request.setCount(count);
                return habitService.complete(member.getId(), habitId, today, request);
            });
        }

        assertThat(ItMembers.runTogether(tasks)).containsOnlyNulls();
        List<HabitCompletion> rows = completionRepository.findRange(member.getId(), today, today);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCount()).isBetween(1, PARALLEL);
    }

    @Test
    void parallelGoalProgress_addsUp() throws Exception {
        Member member = ItMembers.create(memberRepository, walletService);
        CreateGoalRequest goal = new CreateGoalRequest();
        goal.setTitle("Dana darurat");
        goal.setCategory(GoalCategory.FINANCE);
        goal.setIcon("savings");
        goal.setColor("#22C55E");
        goal.setProgressType(GoalProgressType.VALUE);
        goal.setTargetValue(BigDecimal.valueOf(10_000_000));
        String goalId = goalService.createGoal(member.getId(), goal).getData().getId();

        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < PARALLEL; i++) {
            tasks.add(() -> {
                for (int n = 0; n < 5; n++) {
                    GoalProgressRequest request = new GoalProgressRequest();
                    request.setDelta(BigDecimal.valueOf(100_000));
                    goalService.addProgress(member.getId(), goalId, request);
                }
                return null;
            });
        }

        assertThat(ItMembers.runTogether(tasks)).containsOnlyNulls();
        var result = goalService.getGoal(member.getId(), goalId).getData();
        assertThat(result.getCurrentValue()).isEqualByComparingTo(BigDecimal.valueOf(100_000L * 5 * PARALLEL));
        assertThat(result.getProgressPercent()).isEqualTo(20);
    }
}
