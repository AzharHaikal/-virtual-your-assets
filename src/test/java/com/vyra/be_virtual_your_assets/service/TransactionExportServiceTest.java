package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.transaction.PaymentMethod;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.export.ExportFile;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionExportServiceTest {

    private static final String MEMBER = "member-1";

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryService categoryService;
    @Mock private ValidationService validationService;
    @Mock private MemberActivityService memberActivityService;

    private TransactionExportService service;
    private final Member member = new Member();

    @BeforeEach
    void setUp() {
        service = new TransactionExportService(transactionRepository, categoryService, validationService,
                memberActivityService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
        member.setId(MEMBER);
        member.setPhoneNumber("628123");
        member.setTimezone("Asia/Jakarta");
        member.setLanguage(AppLanguage.EN);
        lenient().when(validationService.getMemberById(MEMBER)).thenReturn(member);
    }

    @Test
    void exportCsv_defaultsToThisMonthUpToToday_neutralisesFormulas_andIsLogged() {
        Transaction t = new Transaction();
        t.setLocalDate(LocalDate.of(2026, 10, 5));
        t.setTransactionType(TransactionType.EXPENSE);
        t.setCategoryId("c-1");
        t.setAmount(new BigDecimal("-18000.00"));
        t.setPaymentMethod(PaymentMethod.CASH);
        t.setTransactionDesc("=HYPERLINK(\"http://x\")");
        t.setReferenceNumber("TRX-1");
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 23);
        when(transactionRepository.findForExport(MEMBER, from, to)).thenReturn(List.of(t));
        FinanceCategory kopi = new FinanceCategory();
        kopi.setName("Kopi");
        when(categoryService.byIds(anyCollection())).thenReturn(Map.of("c-1", kopi));

        ExportFile file = service.exportCsv(MEMBER, null, null);

        assertThat(file.fileName()).isEqualTo("vyra-transactions-2026-10-01_2026-10-23.csv");
        assertThat(file.contentType()).isEqualTo("text/csv");
        String csv = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿\"Date\",\"Type\",\"Category\",\"Amount\"");
        assertThat(csv).contains("\"2026-10-05\",\"EXPENSE\",\"Kopi\",\"18000.00\",\"CASH\",\"'=HYPERLINK(\"\"http://x\"\")\",\"TRX-1\"");
        verify(memberActivityService).createMemberActivity("628123", MemberActivityEvent.EXPORT_TRANSACTIONS);
    }

    @Test
    void exportCsv_moreThan366Days_orReversedRange_isRejected() {
        BadRequestException tooLong = assertThrows(BadRequestException.class,
                () -> service.exportCsv(MEMBER, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
        assertThat(tooLong.getErrorConstant()).isEqualTo(ErrorConstant.EXPORT_RANGE_TOO_LONG);

        BadRequestException reversed = assertThrows(BadRequestException.class,
                () -> service.exportCsv(MEMBER, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)));
        assertThat(reversed.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_DATE_RANGE);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void csvHeader_followsTheMembersLanguage_andSafeKeepsPlainText() {
        member.setLanguage(AppLanguage.ID);
        String csv = new String(TransactionCsvWriter.write(List.of(), Map.of(), AppLanguage.ID), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Tanggal\",\"Jenis\",\"Kategori\"");
        assertThat(TransactionCsvWriter.safe("Makan siang")).isEqualTo("Makan siang");
        assertThat(TransactionCsvWriter.safe("+62 bayar")).isEqualTo("'+62 bayar");
        assertThat(TransactionCsvWriter.safe(null)).isEmpty();
    }
}
