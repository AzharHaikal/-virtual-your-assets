package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Export of all the member's personal data. Requested with the PIN (with the usual lock-out) at most
 * once per day; built in the background as a ZIP that can be downloaded for 24 hours, from the app or
 * from the e-mailed link (which carries a one-off random token, stored only as its SHA-256).
 */
@Service
@Slf4j
public class DataExportService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DataExportRepository exportRepository;
    private final DataExportWriter writer;
    private final ValidationService validationService;
    private final LockoutService lockoutService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MemberActivityService memberActivityService;
    private final EmailClient emailClient;
    private final TimeService timeService;
    private final Duration ttl;
    private final Duration minInterval;
    private final String publicBaseUrl;

    public DataExportService(DataExportRepository exportRepository, DataExportWriter writer,
                             ValidationService validationService, LockoutService lockoutService,
                             BCryptPasswordEncoder passwordEncoder, MemberActivityService memberActivityService,
                             EmailClient emailClient, TimeService timeService,
                             @Value("${vyra.export.ttl-hours:24}") long ttlHours,
                             @Value("${vyra.export.min-interval-hours:24}") long minIntervalHours,
                             @Value("${vyra.public-base-url:http://localhost:8000}") String publicBaseUrl) {
        this.exportRepository = exportRepository;
        this.writer = writer;
        this.validationService = validationService;
        this.lockoutService = lockoutService;
        this.passwordEncoder = passwordEncoder;
        this.memberActivityService = memberActivityService;
        this.emailClient = emailClient;
        this.timeService = timeService;
        this.ttl = Duration.ofHours(ttlHours);
        this.minInterval = Duration.ofHours(minIntervalHours);
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    // =========================================================================
    // Member
    // =========================================================================

    /** Checks the PIN and queues a new export (one per day; a failed one does not count). */
    @Transactional(noRollbackFor = {BusinessException.class, TooManyRequestsException.class})
    public BaseResponse<DataExportResponse> request(String memberId, String pin) {
        Member member = validationService.getMemberById(memberId);
        if (member.getStatus() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }
        lockoutService.assertPinNotLocked(member);
        if (pin == null || !passwordEncoder.matches(pin, member.getPin())) {
            memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_DATA_EXPORT);
            lockoutService.onPinFailure(member); // throws 429 when this attempt reaches the limit
            throw new BusinessException(ErrorConstant.INVALID_PIN_MISS_MATCH);
        }
        lockoutService.onPinSuccess(member);

        LocalDateTime now = timeService.nowUtc();
        exportRepository.findTopByMemberIdOrderByRequestedAtDesc(memberId)
                .filter(last -> last.getStatus() != DataExportStatus.FAILED)
                .map(last -> last.getRequestedAt().plus(minInterval))
                .filter(next -> next.isAfter(now))
                .ifPresent(next -> {
                    throw new TooManyRequestsException(ErrorConstant.DATA_EXPORT_TOO_SOON,
                            Duration.between(now, next).getSeconds() + 1);
                });

        DataExport export = new DataExport();
        export.setMemberId(memberId);
        export.setStatus(DataExportStatus.PENDING);
        export.setRequestedAt(now);
        exportRepository.save(export);
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.REQUEST_DATA_EXPORT);
        log.info("Data export requested. memberId: {} exportId: {}", memberId, export.getId());
        return new BaseResponse<>(ErrorConstant.DATA_EXPORT_REQUESTED.getCode(),
                ErrorConstant.DATA_EXPORT_REQUESTED.getMessage(), toResponse(export));
    }

    /** The latest export; {@code data} is null when there is none. */
    @Transactional(readOnly = true)
    public BaseResponse<DataExportResponse> getLatest(String memberId) {
        return new BaseResponse<>(ErrorConstant.DATA_EXPORT_GET_SUCCESS.getCode(),
                ErrorConstant.DATA_EXPORT_GET_SUCCESS.getMessage(),
                exportRepository.findTopByMemberIdOrderByRequestedAtDesc(memberId).map(DataExportService::toResponse)
                        .orElse(null));
    }

    @Transactional(readOnly = true)
    public BaseResponse<DataExportResponse> get(String memberId, String exportId) {
        DataExport export = exportRepository.findByIdAndMemberId(exportId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.DATA_EXPORT_NOT_FOUND));
        return new BaseResponse<>(ErrorConstant.DATA_EXPORT_GET_SUCCESS.getCode(),
                ErrorConstant.DATA_EXPORT_GET_SUCCESS.getMessage(), toResponse(export));
    }

    /** Download from the app (signed in). */
    @Transactional(readOnly = true)
    public ExportFile download(String memberId, String exportId) {
        DataExport export = exportRepository.findByIdAndMemberId(exportId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.DATA_EXPORT_NOT_FOUND));
        return file(export);
    }

    /** Download from the e-mailed link: the token must match; a wrong one looks like a missing export. */
    @Transactional(readOnly = true)
    public ExportFile downloadWithToken(String exportId, String token) {
        DataExport export = exportRepository.findById(exportId)
                .filter(e -> e.getDownloadTokenHash() != null && token != null
                        && MessageDigest.isEqual(e.getDownloadTokenHash().getBytes(StandardCharsets.US_ASCII),
                                sha256(token).getBytes(StandardCharsets.US_ASCII)))
                .or(() -> exportRepository.findById(exportId).filter(e -> e.getStatus() == DataExportStatus.EXPIRED))
                .orElseThrow(() -> new NotFoundException(ErrorConstant.DATA_EXPORT_NOT_FOUND));
        return file(export);
    }

    private ExportFile file(DataExport export) {
        if (export.getStatus() == DataExportStatus.EXPIRED
                || (export.getExpiresAt() != null && !export.getExpiresAt().isAfter(timeService.nowUtc()))) {
            throw new GoneException(ErrorConstant.DATA_EXPORT_EXPIRED);
        }
        if (export.getStatus() != DataExportStatus.READY || export.getContent() == null) {
            throw new BadRequestException(ErrorConstant.DATA_EXPORT_NOT_READY);
        }
        Member member = validationService.getMemberById(export.getMemberId());
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.DOWNLOAD_DATA_EXPORT);
        return new ExportFile("vyra-data-" + export.getRequestedAt().toLocalDate() + ".zip", "application/zip",
                export.getContent());
    }

    // =========================================================================
    // Background (DataExportJob)
    // =========================================================================

    /** Builds a queued export, stores it for {@code ttl} and e-mails the link. Never throws. */
    public void process(String exportId) {
        DataExport export = exportRepository.findById(exportId).orElse(null);
        if (export == null || export.getStatus() != DataExportStatus.PENDING) {
            return;
        }
        LocalDateTime started = timeService.nowUtc();
        export.setStatus(DataExportStatus.PROCESSING);
        export.setStartedAt(started);
        exportRepository.save(export);

        Member member;
        String token;
        try {
            member = validationService.getMemberById(export.getMemberId());
            byte[] zip = writer.write(member);
            token = newToken();
            LocalDateTime done = timeService.nowUtc();
            export.setContent(zip);
            export.setSizeBytes((long) zip.length);
            export.setDownloadTokenHash(sha256(token));
            export.setCompletedAt(done);
            export.setExpiresAt(done.plus(ttl));
            export.setStatus(DataExportStatus.READY);
            exportRepository.save(export);
        } catch (Exception e) {
            log.warn("Data export failed. exportId: {} reason: {}", exportId, e.getClass().getSimpleName());
            export.setStatus(DataExportStatus.FAILED);
            export.setError(e.getClass().getSimpleName());
            exportRepository.save(export);
            return;
        }
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.DATA_EXPORT_READY);
        try {
            emailClient.sendDataExportEmail(member.getFirstName(), member.getEmail(),
                    publicBaseUrl + "/api/v1/exports/" + export.getId() + "/download?token=" + token, member.getLanguage());
        } catch (RuntimeException e) {
            // The export stays downloadable from the app.
            log.warn("Data export e-mail failed. exportId: {}", exportId);
        }
        log.info("Data export ready. exportId: {} sizeBytes: {}", exportId, export.getSizeBytes());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static DataExportResponse toResponse(DataExport export) {
        DataExportResponse response = new DataExportResponse();
        response.setId(export.getId());
        response.setStatus(export.getStatus());
        response.setRequestedAt(export.getRequestedAt());
        response.setCompletedAt(export.getCompletedAt());
        response.setExpiresAt(export.getStatus() == DataExportStatus.READY ? export.getExpiresAt() : null);
        response.setSizeBytes(export.getSizeBytes());
        return response;
    }
}
