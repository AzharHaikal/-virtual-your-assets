package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MoodEntry;
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import com.vyra.be_virtual_your_assets.repository.GoalMilestoneRepository;
import com.vyra.be_virtual_your_assets.repository.GoalProgressLogRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import com.vyra.be_virtual_your_assets.repository.HabitCompletionRepository;
import com.vyra.be_virtual_your_assets.repository.HabitRepository;
import com.vyra.be_virtual_your_assets.repository.MoodEntryRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportWriterTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private FinanceCategoryRepository categoryRepository;
    @Mock private CategoryService categoryService;
    @Mock private BudgetRepository budgetRepository;
    @Mock private MoodEntryRepository moodEntryRepository;
    @Mock private HabitRepository habitRepository;
    @Mock private HabitCompletionRepository completionRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private GoalMilestoneRepository milestoneRepository;
    @Mock private GoalProgressLogRepository progressLogRepository;

    @Test
    void zipHoldsEveryModule_withTheJournal_butNeverThePin() throws IOException {
        DataExportWriter writer = new DataExportWriter(transactionRepository, categoryRepository, categoryService,
                budgetRepository, moodEntryRepository, habitRepository, completionRepository, goalRepository,
                milestoneRepository, progressLogRepository, new ObjectMapper().registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        Member member = new Member();
        member.setId("m-1");
        member.setFirstName("Azhar");
        member.setPin("$2a$10$secret-hash");
        member.setLanguage(AppLanguage.EN);
        MoodEntry entry = new MoodEntry();
        entry.setLocalDate(LocalDate.of(2026, 10, 20));
        entry.setMoodScore((short) 4);
        entry.setNote("Hari yang baik");
        when(moodEntryRepository.findRange(eq("m-1"), any(), any())).thenReturn(List.of(entry));

        Map<String, String> files = unzip(writer.write(member));

        assertThat(files.keySet()).containsExactlyInAnyOrder("README.txt", "profile.json", "transactions.csv",
                "categories.json", "budgets.json", "mood_entries.json", "habits.json", "goals.json");
        assertThat(files.get("mood_entries.json")).contains("Hari yang baik").contains("2026-10-20");
        assertThat(files.get("profile.json")).contains("Azhar").doesNotContain("secret-hash").doesNotContain("pin");
    }

    private static Map<String, String> unzip(byte[] zip) throws IOException {
        Map<String, String> files = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                files.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }
}
