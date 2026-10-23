package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.GoalCategory;
import com.vyra.be_virtual_your_assets.constant.GoalProgressType;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.CreateMilestoneRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalProgressRequest;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateGoalRequest;
import com.vyra.be_virtual_your_assets.dto.goal.UpdateMilestoneRequest;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.habit.UpdateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.mood.CreateMoodTagRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.service.CategoryService;
import com.vyra.be_virtual_your_assets.service.GoalService;
import com.vyra.be_virtual_your_assets.service.HabitService;
import com.vyra.be_virtual_your_assets.service.MoodService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Every Phase 1 endpoint that takes an id refuses another member's data with 404 (NotFoundException),
 * on a real PostgreSQL, and nothing of the owner changes. Controllers take the member id only from the
 * token, so the service call with another member id is exactly what such a request does.
 * Opt-in, see {@link SchemaMigrationIT} for how to run it ({@code -Dvyra.it.db=true}).
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class CrossAccountAccessIT {

    @Autowired private MemberRepository memberRepository;
    @Autowired private WalletService walletService;
    @Autowired private WalletStatementRepository walletStatementRepository;
    @Autowired private TimeService timeService;
    @Autowired private TransactionService transactionService;
    @Autowired private CategoryService categoryService;
    @Autowired private MoodService moodService;
    @Autowired private HabitService habitService;
    @Autowired private GoalService goalService;

    @Test
    void anotherMembersData_isAlways404_andStaysUnchanged() {
        Member owner = ItMembers.create(memberRepository, walletService);
        Member other = ItMembers.create(memberRepository, walletService);
        String me = other.getId();
        LocalDate today = timeService.today(timeService.zoneOf(owner));

        // Owner's data
        CreateTransactionRequest tx = new CreateTransactionRequest();
        tx.setType(TransactionType.INCOME);
        tx.setCategory(TransactionCategory.SALARY);
        tx.setAmount(BigDecimal.valueOf(1000));
        String txId = transactionService.createTransaction(owner.getId(), tx).getData().getTransactionId();

        CreateCategoryRequest category = new CreateCategoryRequest();
        category.setName("Kopi");
        category.setType(TransactionType.EXPENSE);
        category.setIcon("local_cafe");
        category.setColor("#8B5CF6");
        String categoryId = categoryService.createCategory(owner.getId(), category).getData().getId();

        CreateMoodTagRequest tag = new CreateMoodTagRequest();
        tag.setName("Ngopi");
        String tagId = moodService.createTag(owner.getId(), tag).getData().getId();

        CreateHabitRequest habit = new CreateHabitRequest();
        habit.setName("Lari");
        habit.setCategory(HabitCategory.HEALTH);
        habit.setIcon("directions_run");
        habit.setColor("#22C55E");
        habit.setScheduleType(HabitScheduleType.DAILY);
        String habitId = habitService.createHabit(owner.getId(), habit).getData().getId();

        CreateGoalRequest goal = new CreateGoalRequest();
        goal.setTitle("Belajar");
        goal.setCategory(GoalCategory.EDUCATION);
        goal.setIcon("school");
        goal.setColor("#3B82F6");
        goal.setProgressType(GoalProgressType.MILESTONE);
        goal.setMilestones(List.of("Modul 1"));
        GoalResponse createdGoal = goalService.createGoal(owner.getId(), goal).getData();
        String goalId = createdGoal.getId();
        String milestoneId = createdGoal.getMilestones().get(0).getId();

        // Another member tries every id-based operation.
        UpdateTransactionRequest editTx = new UpdateTransactionRequest();
        editTx.setAmount(BigDecimal.ONE);
        UpdateCategoryRequest editCategory = new UpdateCategoryRequest();
        editCategory.setName("Hacked");
        UpdateHabitRequest editHabit = new UpdateHabitRequest();
        editHabit.setName("Hacked");
        UpdateGoalRequest editGoal = new UpdateGoalRequest();
        editGoal.setTitle("Hacked");
        GoalProgressRequest progress = new GoalProgressRequest();
        progress.setNote("Hacked");
        CreateMilestoneRequest milestone = new CreateMilestoneRequest();
        milestone.setTitle("Hacked");
        UpdateMilestoneRequest tick = new UpdateMilestoneRequest();
        tick.setCompleted(true);

        List<Executable> calls = List.of(
                () -> transactionService.updateOwnTransaction(me, txId, editTx),
                () -> transactionService.deleteOwnTransaction(me, txId),
                () -> categoryService.updateCategory(me, categoryId, editCategory),
                () -> categoryService.deleteCategory(me, categoryId),
                () -> moodService.deleteTag(me, tagId),
                () -> moodService.deleteEntry(me, today),
                () -> habitService.updateHabit(me, habitId, editHabit),
                () -> habitService.deleteHabit(me, habitId),
                () -> habitService.setArchived(me, habitId, true),
                () -> habitService.complete(me, habitId, today, null),
                () -> habitService.uncomplete(me, habitId, today),
                () -> goalService.getGoal(me, goalId),
                () -> goalService.updateGoal(me, goalId, editGoal),
                () -> goalService.deleteGoal(me, goalId),
                () -> goalService.addProgress(me, goalId, progress),
                () -> goalService.addMilestone(me, goalId, milestone),
                () -> goalService.updateMilestone(me, goalId, milestoneId, tick),
                () -> goalService.deleteMilestone(me, goalId, milestoneId));
        for (int i = 0; i < calls.size(); i++) {
            assertThrows(NotFoundException.class, calls.get(i), "call #" + i + " should be 404");
        }

        // Nothing of the owner changed, and the other member sees none of it in their lists.
        WalletStatement wallet = walletStatementRepository.findByPhoneNumber(owner.getPhoneNumber()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("1000");
        assertThat(habitService.getHabits(owner.getId(), null, false).getData())
                .singleElement().satisfies(h -> assertThat(h.getName()).isEqualTo("Lari"));
        GoalResponse ownersGoal = goalService.getGoal(owner.getId(), goalId).getData();
        assertThat(ownersGoal.getTitle()).isEqualTo("Belajar");
        assertThat(ownersGoal.getMilestoneDone()).isZero();
        assertThat(ownersGoal.getProgressLogs()).isEmpty();
        assertThat(goalService.getGoals(me, null).getData()).isEmpty();
        assertThat(habitService.getHabits(me, null, false).getData()).isEmpty();
        assertThat(moodService.getTags(me).getData()).noneMatch(t -> tagId.equals(t.getId()));
    }
}
