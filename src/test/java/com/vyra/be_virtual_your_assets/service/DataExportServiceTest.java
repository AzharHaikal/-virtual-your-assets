package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.dto.export.DataExportResponse;
import com.vyra.be_virtual_your_assets.dto.export.ExportFile;
import com.vyra.be_virtual_your_assets.entity.DataExport;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.GoneException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.DataExportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {

    private static final String MEMBER = "member-1";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 5, 0);

    @Mock private DataExportRepository exportRepository;
    @Mock private DataExportWriter writer;
    @Mock private ValidationService validationService;
    @Mock private LockoutService lockoutService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private MemberActivityService memberActivityService;
    @Mock private EmailClient emailClient;

    private DataExportService service;
    private final Member member = new Member();

    @BeforeEach
    void setUp() {
        service = new DataExportService(exportRepository, writer, validationService, lockoutService, passwordEncoder,
                memberActivityService, emailClient,
                new TimeService(Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"),
                24, 24, "https://api.vyra.test/");
        member.setId(MEMBER);
        member.setFirstName("Azhar");
        member.setEmail("a@b.c");
        member.setPhoneNumber("628123");
        member.setPin("hashed");
        member.setStatus(ActiveStatus.ACTIVE);
        member.setLanguage(AppLanguage.ID);
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
    }

    // =========================================================================
    // request
    // =========================================================================
    @Test
    void request_withTheRightPin_queuesAnExport() {
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        DataExportResponse result = service.request(MEMBER, "123456").getData();

        ArgumentCaptor<DataExport> saved = ArgumentCaptor.forClass(DataExport.class);
        verify(exportRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DataExportStatus.PENDING);
        assertThat(saved.getValue().getRequestedAt()).isEqualTo(NOW);
        assertThat(result.getStatus()).isEqualTo(DataExportStatus.PENDING);
        verify(lockoutService).onPinSuccess(member);
        verify(memberActivityService).createMemberActivity("628123", MemberActivityEvent.REQUEST_DATA_EXPORT);
    }

    @Test
    void request_wrongPin_countsTowardsTheLockout() {
        when(passwordEncoder.matches("000000", "hashed")).thenReturn(false);

        BusinessException wrong = assertThrows(BusinessException.class, () -> service.request(MEMBER, "000000"));

        assertThat(wrong.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_PIN_MISS_MATCH);
        verify(lockoutService).onPinFailure(member);
        verify(memberActivityService).createMemberActivity("628123", MemberActivityEvent.FAILED_DATA_EXPORT);
        verify(exportRepository, never()).save(any());
    }

    @Test
    void request_oncePerDay_butAFailedOneDoesNotCount() {
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        DataExport recent = export(DataExportStatus.READY);
        recent.setRequestedAt(NOW.minusHours(3));
        when(exportRepository.findTopByMemberIdOrderByRequestedAtDesc(MEMBER)).thenReturn(Optional.of(recent));

        TooManyRequestsException tooSoon = assertThrows(TooManyRequestsException.class,
                () -> service.request(MEMBER, "123456"));
        assertThat(tooSoon.getErrorConstant()).isEqualTo(ErrorConstant.DATA_EXPORT_TOO_SOON);
        assertThat(tooSoon.getRetryAfterSeconds()).isEqualTo(21 * 3600 + 1);

        recent.setStatus(DataExportStatus.FAILED);
        service.request(MEMBER, "123456");
        verify(exportRepository).save(any(DataExport.class));
    }

    // =========================================================================
    // process / download
    // =========================================================================
    @Test
    void process_buildsTheZip_storesOnlyTheTokenHash_andEmailsTheLink() throws IOException {
        DataExport export = export(DataExportStatus.PENDING);
        when(exportRepository.findById("e-1")).thenReturn(Optional.of(export));
        when(writer.write(member)).thenReturn(new byte[]{1, 2, 3});

        service.process("e-1");

        assertThat(export.getStatus()).isEqualTo(DataExportStatus.READY);
        assertThat(export.getSizeBytes()).isEqualTo(3);
        assertThat(export.getExpiresAt()).isEqualTo(NOW.plusHours(24));
        assertThat(export.getDownloadTokenHash()).hasSize(64);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailClient).sendDataExportEmail(eq("Azhar"), eq("a@b.c"), link.capture(), eq(AppLanguage.ID));
        assertThat(link.getValue()).startsWith("https://api.vyra.test/api/v1/exports/e-1/download?token=");
        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        assertThat(DataExportService.sha256(token)).isEqualTo(export.getDownloadTokenHash());
    }

    @Test
    void process_aFailure_marksTheExportFailed_andAnEmailFailureKeepsItReady() throws IOException {
        DataExport broken = export(DataExportStatus.PENDING);
        when(exportRepository.findById("e-1")).thenReturn(Optional.of(broken));
        when(writer.write(member)).thenThrow(new IOException("disk"));
        service.process("e-1");
        assertThat(broken.getStatus()).isEqualTo(DataExportStatus.FAILED);
        assertThat(broken.getError()).isEqualTo("IOException");

        DataExport ok = export(DataExportStatus.PENDING);
        ok.setId("e-2");
        when(exportRepository.findById("e-2")).thenReturn(Optional.of(ok));
        doReturn(new byte[]{1}).when(writer).write(member);
        doThrow(new BusinessException(ErrorConstant.EMAIL_SEND_FAILED))
                .when(emailClient).sendDataExportEmail(anyString(), anyString(), anyString(), any());
        service.process("e-2");
        assertThat(ok.getStatus()).isEqualTo(DataExportStatus.READY);
    }

    @Test
    void download_readyExport_fromTheApp_orWithTheToken() {
        DataExport ready = export(DataExportStatus.READY);
        ready.setContent(new byte[]{9});
        ready.setExpiresAt(NOW.plusHours(1));
        ready.setDownloadTokenHash(DataExportService.sha256("secret"));
        when(exportRepository.findByIdAndMemberId("e-1", MEMBER)).thenReturn(Optional.of(ready));
        when(exportRepository.findById("e-1")).thenReturn(Optional.of(ready));

        ExportFile file = service.download(MEMBER, "e-1");
        assertThat(file.contentType()).isEqualTo("application/zip");
        assertThat(file.content()).containsExactly(9);
        assertThat(service.downloadWithToken("e-1", "secret").content()).containsExactly(9);

        NotFoundException wrongToken = assertThrows(NotFoundException.class, () -> service.downloadWithToken("e-1", "guess"));
        assertThat(wrongToken.getErrorConstant()).isEqualTo(ErrorConstant.DATA_EXPORT_NOT_FOUND);
        verify(memberActivityService, org.mockito.Mockito.times(2))
                .createMemberActivity("628123", MemberActivityEvent.DOWNLOAD_DATA_EXPORT);
    }

    @Test
    void download_expired_isGone_andPending_isNotReady() {
        DataExport expired = export(DataExportStatus.READY);
        expired.setContent(new byte[]{9});
        expired.setExpiresAt(NOW.minusMinutes(1));
        when(exportRepository.findByIdAndMemberId("e-1", MEMBER)).thenReturn(Optional.of(expired));
        GoneException gone = assertThrows(GoneException.class, () -> service.download(MEMBER, "e-1"));
        assertThat(gone.getErrorConstant()).isEqualTo(ErrorConstant.DATA_EXPORT_EXPIRED);

        DataExport emptied = export(DataExportStatus.EXPIRED);
        when(exportRepository.findById("e-9")).thenReturn(Optional.of(emptied));
        assertThrows(GoneException.class, () -> service.downloadWithToken("e-9", "old-link"));

        DataExport pending = export(DataExportStatus.PENDING);
        when(exportRepository.findByIdAndMemberId("e-2", MEMBER)).thenReturn(Optional.of(pending));
        BadRequestException notReady = assertThrows(BadRequestException.class, () -> service.download(MEMBER, "e-2"));
        assertThat(notReady.getErrorConstant()).isEqualTo(ErrorConstant.DATA_EXPORT_NOT_READY);
    }

    @Test
    void getLatest_withoutExport_isNull() {
        assertThat(service.getLatest(MEMBER).getData()).isNull();
        assertThrows(NotFoundException.class, () -> service.get(MEMBER, "e-x"));
    }

    private static DataExport export(DataExportStatus status) {
        DataExport export = new DataExport();
        export.setId("e-1");
        export.setMemberId(MEMBER);
        export.setStatus(status);
        export.setRequestedAt(NOW.minusMinutes(5));
        return export;
    }
}
