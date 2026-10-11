package com.vyra.be_virtual_your_assets.inject;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberActivity;
import com.vyra.be_virtual_your_assets.entity.MemberWallet;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberWalletRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Run this class SECOND (after GenerateMemberCsv).
 * Active only on profile "inject".
 *
 * Menggunakan mekanisme Hybrid Batch dengan TransactionTemplate untuk kecepatan maksimal
 * dan toleransi kegagalan per baris.
 */
@Slf4j
@Component
@Profile("inject-member")
@RequiredArgsConstructor
public class InjectionDataMember implements CommandLineRunner {

    private static final String CSV_FILE  = "src/main/java/com/vyra/be_virtual_your_assets/inject/source/members_seed.csv";
    private static final String DELIMITER = ",";
    private static final int BATCH_SIZE   = 500; // Ukuran batch optimal untuk PostgreSQL

    private final MemberRepository        memberRepository;
    private final MemberActivityRepository memberActivityRepository;
    private final MemberWalletRepository  memberWalletRepository;
    private final WalletStatementRepository walletStatementRepository;
    private final PasswordEncoder         passwordEncoder;

    // Menggunakan TransactionManager untuk mengontrol commit/rollback secara manual per batch
    private final PlatformTransactionManager transactionManager;

    // Struktur data helper untuk menampung satu kesatuan data member
    private static class MemberRowDto {
        String phoneNumber, email, firstName, lastName, rawPin;
        LocalDateTime createdAt;
        String[] originalCols; // Untuk log jika gagal

        MemberRowDto(String[] cols, DateTimeFormatter fmt) {
            this.originalCols = cols;
            this.phoneNumber = cols[0].trim();
            this.email       = cols[1].trim().toLowerCase();
            this.firstName   = cols[2].trim();
            this.lastName    = cols[3].trim();
            this.rawPin      = cols[4].trim();
            this.createdAt   = LocalDateTime.parse(cols[5].trim(), fmt);
        }
    }

    @Override
    public void run(String... args) {
        log.info("[INJECT] Starting HIGH-SPEED member data injection from: {}", CSV_FILE);
        long startTime = System.currentTimeMillis();

        int totalSuccess = 0;
        int totalFailed  = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String headerLine = br.readLine(); // Skip header
            if (headerLine == null) {
                log.warn("[INJECT] CSV file is empty.");
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String line;
            List<MemberRowDto> batchList = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] cols = line.split(DELIMITER, -1);
                if (cols.length < 6) {
                    log.warn("[INJECT] Skipping malformed line (columns < 6): {}", line);
                    totalFailed++;
                    continue;
                }

                try {
                    batchList.add(new MemberRowDto(cols, fmt));
                } catch (Exception e) {
                    log.error("[INJECT] ❌ Parsing error line: {} -> {}", line, e.getMessage());
                    totalFailed++;
                    continue;
                }

                // Jika list sudah mencapai batas BATCH_SIZE, langsung proses ke DB
                if (batchList.size() == BATCH_SIZE) {
                    int[] result = processBatchWithFallback(batchList);
                    totalSuccess += result[0];
                    totalFailed  += result[1];
                    batchList.clear();
                    log.info("[INJECT] Progress -> Success: {}, Failed: {}", totalSuccess, totalFailed);
                }
            }

            // Proses sisa data yang belum mencapai ukuran kelipatan BATCH_SIZE
            if (!batchList.isEmpty()) {
                int[] result = processBatchWithFallback(batchList);
                totalSuccess += result[0];
                totalFailed  += result[1];
            }

        } catch (IOException e) {
            log.error("[INJECT] Cannot read CSV file: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[INJECT] ========================================================");
        log.info("[INJECT] COMPLETED in {} ms (~{} seconds)", duration, (duration / 1000));
        log.info("[INJECT] Total Success Realized : {}", totalSuccess);
        log.info("[INJECT] Total Failed / Skipped  : {}", totalFailed);
        log.info("[INJECT] ========================================================");
    }

    /**
     * Memproses batch secara transaksional cepat. Jika di dalam batch ada error (misal data duplikat),
     * maka batch tersebut dibatalkan (rollback) lalu dieksekusi ulang secara eceran (satu per satu)
     * agar data lain dalam batch tersebut tidak ikut gagal.
     */
    private int[] processBatchWithFallback(List<MemberRowDto> batchList) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        int successInBatch = 0;
        int failedInBatch = 0;

        try {
            // Jalankan mode cepat (Semua disave sekaligus dalam 1 transaksi)
            txTemplate.executeWithoutResult(status -> {
                for (MemberRowDto dto : batchList) {
                    executeSingleInsert(dto);
                }
            });
            // Jika sampai sini artinya 500 data sukses total tanpa hambatan
            successInBatch = batchList.size();
        } catch (Exception e) {
            // Terjadi error di salah satu dari 500 data tersebut!
            // Kita fallback: Jalankan eceran satu per satu agar data yang bersih tetap tersimpan.
            for (MemberRowDto dto : batchList) {
                boolean singleSuccess = executeSingleWithIsolatedTransaction(dto);
                if (singleSuccess) {
                    successInBatch++;
                } else {
                    failedInBatch++;
                }
            }
        }

        return new int[]{successInBatch, failedInBatch};
    }

    /**
     * Menyimpan satu baris data secara langsung.
     */
    private void executeSingleInsert(MemberRowDto dto) {
        // 1. Member
        Member member = new Member();
        member.setPhoneNumber(dto.phoneNumber);
        member.setEmail(dto.email);
        member.setFirstName(dto.firstName);
        member.setLastName(dto.lastName);
        member.setPin(passwordEncoder.encode(dto.rawPin));
        member.setStatus(ActiveStatus.ACTIVE);
        member.setCreatedBy(dto.phoneNumber);
        member.setCreatedAt(dto.createdAt);
        memberRepository.save(member);

        // 2. Member Wallet
        MemberWallet memberWallet = new MemberWallet();
        memberWallet.setMemberId(member.getId());
        memberWallet.setPhoneNumber(dto.phoneNumber);
        memberWallet.setStatus(ActiveStatus.ACTIVE);
        memberWallet.setCreatedBy(dto.phoneNumber);
        memberWallet.setCreatedAt(dto.createdAt);
        memberWalletRepository.save(memberWallet);

        // 3. Wallet Statement
        WalletStatement walletStatement = new WalletStatement();
        walletStatement.setMemberWalletId(memberWallet.getId());
        walletStatement.setPhoneNumber(dto.phoneNumber);
        walletStatement.setBalance(BigDecimal.ZERO);
        walletStatement.setTotalIncome(BigDecimal.ZERO);
        walletStatement.setTotalExpense(BigDecimal.ZERO);
        walletStatement.setCreatedBy(dto.phoneNumber);
        walletStatement.setCreatedAt(dto.createdAt);
        walletStatementRepository.save(walletStatement);

        // 4. Member Activity
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.ATTEMPT_REGISTER);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.ATTEMPT_CREATE_WALLET);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.SUCCESS_CREATE_WALLET);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.ATTEMPT_GENERATE_OTP_REGISTER);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.SUCCESS_REGISTER);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.ATTEMPT_VERIFY_OTP);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.SUCCESS_VERIFY_OTP);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.ATTEMPT_LOGIN);
        activity(dto.phoneNumber, dto.createdAt, MemberActivityEvent.SUCCESS_LOGIN);
    }

    /**
     * Fallback method: Menyimpan 1 data dengan Transaksi Terisolasi Mandiri.
     * Jika gagal, tidak merusak data sebelum atau sesudahnya.
     */
    private boolean executeSingleWithIsolatedTransaction(MemberRowDto dto) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        // Set propagation ke REQUIRES_NEW secara programmatic agar membuat transaksi mandiri baru
        txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        try {
            txTemplate.executeWithoutResult(status -> executeSingleInsert(dto));
            return true; // Sukses diinsert eceran
        } catch (Exception e) {
            log.error("[INJECT-FALLBACK] ❌ Row truly failed -> Phone: {} | Email: {} | Error: {}",
                    dto.phoneNumber, dto.email, e.getMessage());
            return false; // Memang datanya yang cacat / duplikat
        }
    }

    private void activity(String phoneNumber, LocalDateTime createdAt, MemberActivityEvent activityEvent) {
        MemberActivity activity = new MemberActivity();
        activity.setPhoneNumber(phoneNumber);
        activity.setEvent(activityEvent.name());
        activity.setDescription(activityEvent.getDescription());
        activity.setCreatedBy(phoneNumber);
        activity.setCreatedAt(createdAt);
        memberActivityRepository.save(activity);
    }
}