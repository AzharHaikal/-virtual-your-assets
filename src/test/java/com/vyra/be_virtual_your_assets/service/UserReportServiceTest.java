package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.constant.UserReportCategory;
import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import com.vyra.be_virtual_your_assets.dto.report.user.CreateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UpdateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UserReportResponse;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.UserReport;
import com.vyra.be_virtual_your_assets.entity.UserReportAttachment;
import com.vyra.be_virtual_your_assets.entity.UserReportNote;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.UserReportAttachmentRepository;
import com.vyra.be_virtual_your_assets.repository.UserReportNoteRepository;
import com.vyra.be_virtual_your_assets.repository.UserReportRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 3, 0);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @Mock private UserReportRepository reportRepository;
    @Mock private UserReportNoteRepository noteRepository;
    @Mock private UserReportAttachmentRepository attachmentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private ValidationService validationService;
    @Mock private NotificationService notificationService;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;

    private UserReportService service;
    private final CustomAdminDetails operator = new CustomAdminDetails("admin-1", "token", AdminRole.OPERATOR);

    @BeforeEach
    void setUp() {
        service = new UserReportService(reportRepository, noteRepository, attachmentRepository, memberRepository,
                adminRepository, validationService, notificationService, adminAuthorizationService,
                adminActivityService, new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void create_withScreenshot_storesItSeparately() {
        when(reportRepository.countByMemberIdAndCreatedAtAfter("m-1", NOW.minusDays(1))).thenReturn(0L);
        doAnswer(inv -> {
            ((UserReport) inv.getArgument(0)).setId("r-1");
            return inv.getArgument(0);
        }).when(reportRepository).saveAndFlush(any());
        CreateUserReportRequest request = request(Base64.getEncoder().encodeToString(PNG));

        UserReportResponse result = service.create("m-1", request).getData();

        assertThat(result.isHasAttachment()).isTrue();
        assertThat(result.getStatus()).isEqualTo(UserReportStatus.OPEN);
        assertThat(result.getMemberEmail()).isNull();
        ArgumentCaptor<UserReportAttachment> attachment = ArgumentCaptor.forClass(UserReportAttachment.class);
        verify(attachmentRepository).save(attachment.capture());
        assertThat(attachment.getValue().getReportId()).isEqualTo("r-1");
        assertThat(attachment.getValue().getContentType()).isEqualTo("image/png");
    }

    @Test
    void create_sixthReportInADay_isTooMany() {
        when(reportRepository.countByMemberIdAndCreatedAtAfter("m-1", NOW.minusDays(1))).thenReturn(5L);

        assertThrows(TooManyRequestsException.class, () -> service.create("m-1", request(null)));
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_attachmentThatIsNoImage_isRejected() {
        when(reportRepository.countByMemberIdAndCreatedAtAfter("m-1", NOW.minusDays(1))).thenReturn(0L);
        String text = Base64.getEncoder().encodeToString("not an image".getBytes());

        assertThrows(BadRequestException.class, () -> service.create("m-1", request(text)));
        assertThrows(BadRequestException.class, () -> service.create("m-1", request("%%%")));
    }

    @Test
    void update_resolve_recordsHistoryAuditsAndTellsTheMember() {
        UserReport report = report();
        when(reportRepository.findById("r-1")).thenReturn(Optional.of(report));
        Member member = new Member();
        member.setId("m-1");
        member.setFirstName("Ana");
        member.setLastName("Putri");
        member.setEmail("ana@example.com");
        member.setLanguage(AppLanguage.ID);
        when(validationService.getMemberById("m-1")).thenReturn(member);
        when(memberRepository.findById("m-1")).thenReturn(Optional.of(member));
        UserReportNote note = new UserReportNote();
        note.setAdminId("admin-1");
        note.setStatus(UserReportStatus.RESOLVED);
        note.setNote("Fixed in 1.3");
        when(noteRepository.findByReportIdOrderByCreatedAtAsc("r-1")).thenReturn(List.of(note));
        when(adminRepository.findAllById(List.of("admin-1"))).thenReturn(List.of(admin("admin-1", AdminRole.OPERATOR)));
        UpdateUserReportRequest request = new UpdateUserReportRequest();
        request.setStatus(UserReportStatus.RESOLVED);
        request.setNote(" Fixed in 1.3 ");

        UserReportResponse result = service.update("r-1", request, operator).getData();

        verify(adminAuthorizationService).requirePermission(AdminPermission.REPORT_HANDLE);
        assertThat(report.getStatus()).isEqualTo(UserReportStatus.RESOLVED);
        assertThat(report.getResolvedAt()).isEqualTo(NOW);
        ArgumentCaptor<UserReportNote> saved = ArgumentCaptor.forClass(UserReportNote.class);
        verify(noteRepository).save(saved.capture());
        assertThat(saved.getValue().getNote()).isEqualTo("Fixed in 1.3");
        assertThat(saved.getValue().getStatus()).isEqualTo(UserReportStatus.RESOLVED);
        assertThat(result.getMemberName()).isEqualTo("Ana Putri");
        assertThat(result.getHistory()).singleElement().extracting(UserReportResponse.Note::getAdminName).isEqualTo("Admin admin-1");
        verify(adminActivityService).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.UPDATE_USER_REPORT), anyString(), isNull(), isNull());
        verify(notificationService).notify(eq(member), eq(NotificationType.SYSTEM), eq("USER_REPORT:r-1:RESOLVED"),
                eq(NotificationMessages.reportResolved(AppLanguage.ID)), eq(Map.of("route", "report", "reportId", "r-1")));
    }

    @Test
    void update_assigneeWithoutReportPermission_isRejected() {
        when(reportRepository.findById("r-1")).thenReturn(Optional.of(report()));
        when(adminRepository.findById("viewer")).thenReturn(Optional.of(admin("viewer", AdminRole.VIEWER)));
        UpdateUserReportRequest request = new UpdateUserReportRequest();
        request.setAssignedAdminId("viewer");

        assertThrows(BadRequestException.class, () -> service.update("r-1", request, operator));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void update_emptyAssignee_unassignsWithoutHistoryEntry() {
        UserReport report = report();
        report.setAssignedAdminId("admin-1");
        when(reportRepository.findById("r-1")).thenReturn(Optional.of(report));
        UpdateUserReportRequest request = new UpdateUserReportRequest();
        request.setAssignedAdminId("");

        service.update("r-1", request, operator);

        assertThat(report.getAssignedAdminId()).isNull();
        verify(noteRepository, never()).save(any());
        verify(notificationService, never()).notify(any(), any(), anyString(), any(), any());
    }

    private static CreateUserReportRequest request(String attachment) {
        CreateUserReportRequest request = new CreateUserReportRequest();
        request.setCategory(UserReportCategory.BUG);
        request.setDescription("  The app closes when I open goals.  ");
        request.setAttachmentBase64(attachment);
        request.setAppVersion("1.3.0");
        request.setPlatform("android");
        return request;
    }

    private static UserReport report() {
        UserReport report = new UserReport();
        report.setId("r-1");
        report.setMemberId("m-1");
        report.setCategory(UserReportCategory.BUG);
        report.setDescription("Crash");
        report.setCreatedAt(NOW.minusDays(1));
        return report;
    }

    private static Admin admin(String id, AdminRole role) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setName("Admin " + id);
        admin.setRole(role);
        admin.setStatus(ActiveStatus.ACTIVE);
        return admin;
    }
}
