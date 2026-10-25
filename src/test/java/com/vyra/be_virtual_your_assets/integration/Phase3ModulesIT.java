package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ConsentType;
import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import com.vyra.be_virtual_your_assets.constant.HabitScheduleType;
import com.vyra.be_virtual_your_assets.constant.LegalDocumentType;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.constant.UserReportCategory;
import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementRequest;
import com.vyra.be_virtual_your_assets.dto.category.AdminCategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.habit.CreateHabitRequest;
import com.vyra.be_virtual_your_assets.dto.insight.AdvancedInsightResponse;
import com.vyra.be_virtual_your_assets.dto.legal.PublishLegalDocumentRequest;
import com.vyra.be_virtual_your_assets.dto.metrics.ProductMetricsResponse;
import com.vyra.be_virtual_your_assets.dto.metrics.SystemStatusResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryRequest;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationResponse;
import com.vyra.be_virtual_your_assets.dto.report.user.CreateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UpdateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UserReportResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AiReflection;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.AiReflectionRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminCategoryService;
import com.vyra.be_virtual_your_assets.service.AdvancedInsightService;
import com.vyra.be_virtual_your_assets.service.AiReflectionService;
import com.vyra.be_virtual_your_assets.service.AnnouncementService;
import com.vyra.be_virtual_your_assets.service.ConsentService;
import com.vyra.be_virtual_your_assets.service.HabitService;
import com.vyra.be_virtual_your_assets.service.LegalDocumentService;
import com.vyra.be_virtual_your_assets.service.MoodService;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.ProductMetricsService;
import com.vyra.be_virtual_your_assets.service.TimeService;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import com.vyra.be_virtual_your_assets.service.UserReportService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 modules on a real PostgreSQL: the V26-V28 tables, the native aggregate queries of the product
 * metrics and the admin flows (signed in as the seeded Super Admin). The scheduler is off so only these
 * calls create notifications. Opt-in, see {@link SchemaMigrationIT} ({@code -Dvyra.it.db=true}).
 */
@SpringBootTest(properties = "vyra.scheduler.enabled=false")
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class Phase3ModulesIT {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Autowired private MemberRepository memberRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private WalletService walletService;
    @Autowired private TimeService timeService;
    @Autowired private TransactionService transactionService;
    @Autowired private HabitService habitService;
    @Autowired private MoodService moodService;
    @Autowired private NotificationService notificationService;
    @Autowired private ProductMetricsService productMetricsService;
    @Autowired private UserReportService userReportService;
    @Autowired private AnnouncementService announcementService;
    @Autowired private LegalDocumentService legalDocumentService;
    @Autowired private AdminCategoryService adminCategoryService;
    @Autowired private ConsentService consentService;
    @Autowired private AdvancedInsightService advancedInsightService;
    @Autowired private AiReflectionService aiReflectionService;
    @Autowired private AiReflectionRepository aiReflectionRepository;

    private CustomAdminDetails admin;

    @BeforeEach
    void signInAsSuperAdmin() {
        Admin seeded = adminRepository.findByEmailIgnoreCase("superadmin@vyra.local").orElseThrow();
        admin = new CustomAdminDetails(seeded.getId(), "it-token", AdminRole.SUPER_ADMIN);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void productMetricsAndSystemStatus_runTheirNativeQueries() {
        activeMember();

        ProductMetricsResponse metrics = productMetricsService.getMetrics(30).getData();
        assertThat(metrics.getMembers().getActive()).isPositive();
        assertThat(metrics.getMembers().getNewInPeriod()).isPositive();
        assertThat(metrics.getRetention()).hasSize(3);
        assertThat(metrics.getEngagement().getFinance().getEngaged()).isPositive();
        assertThat(metrics.getEngagement().getMood().getEngaged()).isPositive();
        assertThat(metrics.getEngagement().getHabit().getEligible()).isPositive();

        SystemStatusResponse status = productMetricsService.getStatus().getData();
        assertThat(status.getDatabase().isUp()).isTrue();
        assertThat(status.getStatus()).isIn("UP", "DEGRADED");
        assertThat(status.getScheduler().getJobs()).isNotNull();
    }

    @Test
    void userReport_withScreenshot_isHandledAndTheMemberIsTold() {
        Member member = ItMembers.create(memberRepository, walletService);
        CreateUserReportRequest request = new CreateUserReportRequest();
        request.setCategory(UserReportCategory.BUG);
        request.setDescription("Aplikasi tertutup saat membuka goal");
        request.setAttachmentBase64(Base64.getEncoder().encodeToString(PNG));
        String reportId = userReportService.create(member.getId(), request).getData().getId();

        assertThat(userReportService.getOwn(member.getId()).getData()).singleElement()
                .satisfies(r -> assertThat(r.isHasAttachment()).isTrue());
        assertThat(userReportService.getQueue(UserReportStatus.OPEN, 0, 100).getData().getContent())
                .extracting(UserReportResponse::getId).contains(reportId);
        assertThat(userReportService.getAttachment(reportId).getContentType()).isEqualTo("image/png");

        UpdateUserReportRequest update = new UpdateUserReportRequest();
        update.setStatus(UserReportStatus.RESOLVED);
        update.setAssignedAdminId(admin.getAdminId());
        update.setNote("Diperbaiki di versi berikutnya");
        UserReportResponse resolved = userReportService.update(reportId, update, admin).getData();

        assertThat(resolved.getStatus()).isEqualTo(UserReportStatus.RESOLVED);
        assertThat(resolved.getMemberEmail()).isEqualTo(member.getEmail());
        assertThat(resolved.getAssignedAdminName()).isNotBlank();
        assertThat(resolved.getHistory()).singleElement().satisfies(n -> assertThat(n.getNote()).contains("Diperbaiki"));
        List<NotificationResponse> inbox = notificationService.getNotifications(member.getId(), false, 0, 20)
                .getData().getContent();
        assertThat(inbox).anySatisfy(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.SYSTEM);
            assertThat(n.getPayload()).containsEntry("reportId", reportId);
        });
    }

    @Test
    void announcement_isShownAndDeliveredOnce() {
        Member member = ItMembers.create(memberRepository, walletService);
        member.setLanguage(AppLanguage.ID);
        memberRepository.saveAndFlush(member);
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("IT " + UUID.randomUUID());
        request.setBody("Pengumuman dari integration test");
        request.setLanguage("ID");
        request.setStartsAt(timeService.nowUtc().minusMinutes(1) + "Z");
        String id = announcementService.create(request, admin).getData().getId();
        try {
            announcementService.publish(id, admin);
            assertThat(announcementService.getActive(member.getId()).getData())
                    .extracting(a -> a.getId()).contains(id);

            assertThat(announcementService.deliverDue(timeService.nowUtc())).isPositive();
            assertThat(announcementService.deliverDue(timeService.nowUtc())).isZero();
            assertThat(notificationService.getNotifications(member.getId(), false, 0, 20).getData().getContent())
                    .anySatisfy(n -> assertThat(n.getType()).isEqualTo(NotificationType.ANNOUNCEMENT));
        } finally {
            announcementService.delete(id, admin);
        }
        assertThat(announcementService.getActive(member.getId()).getData()).extracting(a -> a.getId()).doesNotContain(id);
    }

    @Test
    void legalDocuments_areVersioned_andCategoriesToggle() {
        PublishLegalDocumentRequest legal = new PublishLegalDocumentRequest();
        legal.setType(LegalDocumentType.TERMS);
        legal.setLanguage(AppLanguage.EN);
        legal.setContent("Terms v-a");
        int first = legalDocumentService.publish(legal, admin).getData().getVersion();
        legal.setContent("Terms v-b");
        int second = legalDocumentService.publish(legal, admin).getData().getVersion();

        assertThat(second).isEqualTo(first + 1);
        assertThat(legalDocumentService.getLatest(LegalDocumentType.TERMS, AppLanguage.EN).getData().getContent())
                .isEqualTo("Terms v-b");
        assertThat(legalDocumentService.getVersions(LegalDocumentType.TERMS, AppLanguage.EN).getData())
                .allSatisfy(v -> assertThat(v.getContent()).isNull());

        CreateCategoryRequest category = new CreateCategoryRequest();
        category.setName("IT " + UUID.randomUUID().toString().substring(0, 8));
        category.setType(TransactionType.EXPENSE);
        category.setIcon("pets");
        category.setColor("#ff8800");
        AdminCategoryResponse created = adminCategoryService.create(category, admin).getData();
        assertThat(adminCategoryService.setActive(created.getId(), false, admin).getData().isActive()).isFalse();
        assertThat(adminCategoryService.setActive(created.getId(), true, admin).getData().isActive()).isTrue();
        assertThat(adminCategoryService.getCategories().getData()).extracting(AdminCategoryResponse::getId)
                .contains(created.getId());
        adminCategoryService.setActive(created.getId(), false, admin);
    }

    @Test
    void consent_reflectionStorage_andAdvancedInsights() {
        Member member = activeMember();

        assertThat(consentService.isGranted(member.getId(), ConsentType.AI_REFLECTION)).isFalse();
        consentService.set(member.getId(), "AI_REFLECTION", true);
        assertThat(consentService.isGranted(member.getId(), ConsentType.AI_REFLECTION)).isTrue();
        consentService.set(member.getId(), "AI_REFLECTION", false);
        assertThat(consentService.getAll(member.getId()).getData()).singleElement()
                .satisfies(c -> assertThat(c.getRevokedAt()).isNotNull());

        // The text is encrypted at rest and read back as written.
        AiReflection reflection = new AiReflection();
        reflection.setMemberId(member.getId());
        reflection.setPeriodStart(LocalDate.of(2026, 10, 12));
        reflection.setPeriodEnd(LocalDate.of(2026, 10, 18));
        reflection.setContent("Minggu yang stabil.");
        reflection.setModel("claude-opus-5-5");
        reflection.setCreatedAt(timeService.nowUtc());
        aiReflectionRepository.saveAndFlush(reflection);
        assertThat(aiReflectionService.getAll(member.getId()).getData()).singleElement()
                .satisfies(r -> assertThat(r.getContent()).isEqualTo("Minggu yang stabil."));
        aiReflectionService.delete(member.getId(), reflection.getId());
        assertThat(aiReflectionRepository.findById(reflection.getId())).isEmpty();

        assertThat(aiReflectionService.getStatus(member.getId()).getData().isConsentGiven()).isFalse();

        AdvancedInsightResponse insight = advancedInsightService.get(member.getId()).getData();
        assertThat(insight.getMonths()).hasSize(12);
        assertThat(insight.getMonths().get(11).getExpense()).isEqualByComparingTo("25000");
    }

    /** A member with a transaction, a mood check-in and a habit done today. */
    private Member activeMember() {
        Member member = ItMembers.create(memberRepository, walletService);
        CreateTransactionRequest expense = new CreateTransactionRequest();
        expense.setType(TransactionType.EXPENSE);
        expense.setCategory(TransactionCategory.FOOD_AND_BEVERAGE);
        expense.setAmount(BigDecimal.valueOf(25_000));
        expense.setTransactionDesc("Makan");
        transactionService.createTransaction(member.getId(), expense);

        LocalDate today = timeService.today(timeService.zoneOf(member));
        MoodEntryRequest mood = new MoodEntryRequest();
        mood.setMoodScore(4);
        moodService.saveEntry(member.getId(), today, mood);

        CreateHabitRequest habit = new CreateHabitRequest();
        habit.setName("Minum air");
        habit.setCategory(HabitCategory.HEALTH);
        habit.setIcon("local_drink");
        habit.setColor("#22C55E");
        habit.setScheduleType(HabitScheduleType.DAILY);
        String habitId = habitService.createHabit(member.getId(), habit).getData().getId();
        habitService.complete(member.getId(), habitId, today, null);
        return member;
    }
}
