package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.entity.Budget;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalMilestone;
import com.vyra.be_virtual_your_assets.entity.GoalProgressLog;
import com.vyra.be_virtual_your_assets.entity.Habit;
import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.entity.MoodTag;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import com.vyra.be_virtual_your_assets.repository.GoalMilestoneRepository;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the ZIP of a member's personal data: profile, transactions (CSV), own categories, budgets, mood
 * entries with the journal (decrypted), habits with their completions, goals with milestones and progress.
 * Secrets (PIN hash, tokens) are never included. Deleted items are left out.
 */
@Component
@RequiredArgsConstructor
public class DataExportWriter {

    private static final LocalDate FIRST_DAY = LocalDate.of(1900, 1, 1);
    private static final LocalDate LAST_DAY = LocalDate.of(9999, 12, 31);

    private final TransactionRepository transactionRepository;
    private final FinanceCategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final BudgetRepository budgetRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;
    private final GoalRepository goalRepository;
    private final GoalMilestoneRepository milestoneRepository;
    private final GoalProgressLogRepository progressLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public byte[] write(Member member) throws IOException {
        String memberId = member.getId();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            entry(zip, "README.txt", readme().getBytes(StandardCharsets.UTF_8));
            json(zip, "profile.json", profile(member));

            List<Transaction> transactions = transactionRepository.findForExport(memberId, FIRST_DAY, LAST_DAY);
            entry(zip, "transactions.csv", TransactionCsvWriter.write(transactions,
                    categoryService.byIds(transactions.stream().map(Transaction::getCategoryId).toList()),
                    member.getLanguage()));
            json(zip, "categories.json", categories(memberId));
            json(zip, "budgets.json", budgets(memberId));
            json(zip, "mood_entries.json", moodEntries(memberId));
            json(zip, "habits.json", habits(memberId));
            json(zip, "goals.json", goals(memberId));
        }
        return out.toByteArray();
    }

    private static String readme() {
        return """
                VYRA - export of your personal data / ekspor data pribadi Anda

                profile.json       your profile and preferences / profil dan preferensi
                transactions.csv   your transactions / transaksi
                categories.json    your own categories / kategori buatan Anda
                budgets.json       your budgets / anggaran
                mood_entries.json  mood check-ins and journal / mood dan jurnal
                habits.json        habits and the days you did them / kebiasaan dan riwayatnya
                goals.json         goals, milestones and progress / tujuan, milestone dan progres

                Dates are your own local dates; times are UTC (ending in Z).
                Keep this file private: it contains your journal.
                """;
    }

    private static Map<String, Object> profile(Member member) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", member.getId());
        profile.put("firstName", member.getFirstName());
        profile.put("lastName", member.getLastName());
        profile.put("email", member.getEmail());
        profile.put("phoneNumber", member.getPhoneNumber());
        profile.put("timezone", member.getTimezone());
        profile.put("language", member.getLanguage());
        profile.put("theme", member.getTheme());
        profile.put("currency", member.getCurrency());
        profile.put("createdAt", member.getCreatedAt());
        return profile;
    }

    private List<Map<String, Object>> categories(String memberId) {
        return categoryRepository.findVisible(memberId).stream()
                .filter(c -> !c.isBuiltIn())
                .map(c -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", c.getId());
                    item.put("name", c.getName());
                    item.put("type", c.getType());
                    item.put("icon", c.getIcon());
                    item.put("color", c.getColor());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> budgets(String memberId) {
        List<Budget> budgets = budgetRepository.findByMemberIdAndDeletedAtIsNullOrderByPeriodStartAscCreatedAtAsc(memberId);
        Map<String, FinanceCategory> categories = categoryService.byIds(budgets.stream().map(Budget::getCategoryId).toList());
        return budgets.stream().map(b -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", categories.containsKey(b.getCategoryId())
                    ? categories.get(b.getCategoryId()).getName() : b.getCategoryId());
            item.put("periodStart", b.getPeriodStart());
            item.put("periodEnd", b.getPeriodEnd());
            item.put("amount", b.getAmount());
            return item;
        }).toList();
    }

    private List<Map<String, Object>> moodEntries(String memberId) {
        List<MoodEntry> entries = new ArrayList<>(moodEntryRepository.findRange(memberId, FIRST_DAY, LAST_DAY));
        entries.sort(Comparator.comparing(MoodEntry::getLocalDate));
        return entries.stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", e.getLocalDate());
            item.put("moodScore", e.getMoodScore());
            item.put("tags", e.getTags().stream().map(MoodTag::getName).sorted().toList());
            item.put("journal", e.getNote());
            item.put("checkedInAt", e.getCheckedInAt());
            return item;
        }).toList();
    }

    private List<Map<String, Object>> habits(String memberId) {
        Map<String, List<HabitCompletion>> completions = completionRepository.findRange(memberId, FIRST_DAY, LAST_DAY)
                .stream().collect(Collectors.groupingBy(HabitCompletion::getHabitId));
        return habitRepository.findAllOfMember(memberId).stream().map(h -> {
            Map<String, Object> item = habit(h);
            item.put("completions", completions.getOrDefault(h.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(HabitCompletion::getLocalDate))
                    .map(c -> {
                        Map<String, Object> day = new LinkedHashMap<>();
                        day.put("date", c.getLocalDate());
                        day.put("count", c.getCount());
                        return day;
                    })
                    .toList());
            return item;
        }).toList();
    }

    private static Map<String, Object> habit(Habit h) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", h.getName());
        item.put("category", h.getCategory());
        item.put("targetCount", h.getTargetCount());
        item.put("scheduleDays", h.scheduledDays());
        item.put("startDate", h.getStartDate());
        item.put("archived", h.isArchived());
        item.put("reminderEnabled", h.isReminderEnabled());
        item.put("reminderTime", h.getReminderTime());
        return item;
    }

    private List<Map<String, Object>> goals(String memberId) {
        List<Goal> goals = goalRepository.findAllOfMember(memberId);
        List<String> ids = goals.stream().map(Goal::getId).toList();
        Map<String, List<GoalMilestone>> milestones = ids.isEmpty() ? Map.of()
                : milestoneRepository.findAllOfGoals(ids).stream().collect(Collectors.groupingBy(GoalMilestone::getGoalId));
        Map<String, List<GoalProgressLog>> logs = ids.isEmpty() ? Map.of()
                : progressLogRepository.findByGoalIdInOrderByCreatedAtAsc(ids).stream()
                        .collect(Collectors.groupingBy(GoalProgressLog::getGoalId));
        return goals.stream().map(g -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", g.getTitle());
            item.put("description", g.getDescription());
            item.put("category", g.getCategory());
            item.put("progressType", g.getProgressType());
            item.put("targetValue", g.getTargetValue());
            item.put("currentValue", g.getCurrentValue());
            item.put("unit", g.getUnit());
            item.put("dueDate", g.getDueDate());
            item.put("status", g.getStatus());
            item.put("linkedToFinance", g.isLinkedToFinance());
            item.put("milestones", milestones.getOrDefault(g.getId(), List.of()).stream().map(m -> {
                Map<String, Object> milestone = new LinkedHashMap<>();
                milestone.put("title", m.getTitle());
                milestone.put("completedAt", m.getCompletedAt());
                return milestone;
            }).toList());
            item.put("progress", logs.getOrDefault(g.getId(), List.of()).stream().map(l -> {
                Map<String, Object> log = new LinkedHashMap<>();
                log.put("delta", l.getDelta());
                log.put("valueAfter", l.getValueAfter());
                log.put("note", l.getNote());
                log.put("createdAt", l.getCreatedAt());
                return log;
            }).toList());
            return item;
        }).toList();
    }

    private void json(ZipOutputStream zip, String name, Object value) throws IOException {
        entry(zip, name, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value));
    }

    private static void entry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}
