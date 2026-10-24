package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.dto.export.ExportFile;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** CSV report of the member's own transactions for a period (Keuangan > Ekspor). */
@Service
@RequiredArgsConstructor
public class TransactionExportService {

    static final int MAX_DAYS = 366;

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final ValidationService validationService;
    private final MemberActivityService memberActivityService;
    private final TimeService timeService;

    /** SUCCESS transactions of [from, to] (default: the current month up to today), at most 366 days. */
    @Transactional(readOnly = true)
    public ExportFile exportCsv(String memberId, LocalDate from, LocalDate to) {
        Member member = validationService.getMemberById(memberId);
        LocalDate today = timeService.today(timeService.zoneOf(member));
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        if (start.isAfter(end)) {
            throw new BadRequestException(ErrorConstant.INVALID_DATE_RANGE);
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new BadRequestException(ErrorConstant.EXPORT_RANGE_TOO_LONG);
        }
        List<Transaction> transactions = transactionRepository.findForExport(memberId, start, end);
        byte[] csv = TransactionCsvWriter.write(transactions,
                categoryService.byIds(transactions.stream().map(Transaction::getCategoryId).toList()), member.getLanguage());
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.EXPORT_TRANSACTIONS);
        return new ExportFile("vyra-transactions-" + start + "_" + end + ".csv", "text/csv", csv);
    }
}
