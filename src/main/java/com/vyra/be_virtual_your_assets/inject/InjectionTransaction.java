package com.vyra.be_virtual_your_assets.inject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Run this class THIRD (after InjectionDataMember).
 * Mengambil 1000 data member lama dan memanggil PostgreSQL Function secara sekuensial.
 */
@Slf4j
@Component
@Profile("inject-transaction")
@RequiredArgsConstructor
public class InjectionTransaction implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("[INJECT-TX] Starting transaction injection for 1000 members...");

        // 1. Ambil data 1000 member menggunakan native query melalui JdbcTemplate
        String selectQuery = """
            SELECT
                m.id AS member_id,
                ws.member_wallet_id AS member_wallet_id,
                ws.id AS wallet_statement_id,
                m.email AS email,
                m.phone_number AS phone_number
            FROM
                idp."member" m
            JOIN wallet.wallet_statement ws ON m.phone_number = ws.phone_number
            ORDER BY m.created_at ASC
            LIMIT 1000
        """;

        List<Map<String, Object>> memberList = jdbcTemplate.queryForList(selectQuery);

        if (memberList.isEmpty()) {
            log.warn("[INJECT-TX] No members found to inject transactions.");
            return;
        }

        log.info("[INJECT-TX] Found {} members. Processing stored functions...", memberList.size());

        int success = 0;
        int failed = 0;

        // 2. Query untuk memanggil PostgreSQL Stored Function
        String functionQuery = "SELECT transaction_finance.inject_transactions(?, ?, ?, ?, ?, ?)";

        // 3. Loop data member dan panggil function database satu per satu
        for (Map<String, Object> member : memberList) {
            // Ambil data dengan aman dari Map (menghindari NullPointerException)
            Object memberId         = member.get("member_id");
            Object memberWalletId   = member.get("member_wallet_id");
            Object walletStatementId = member.get("wallet_statement_id");
            String email            = (String) member.get("email");
            String phoneNumber      = (String) member.get("phone_number");

            try {
                // Eksekusi pemanggilan function postgres
                jdbcTemplate.execute(
                        functionQuery,
                        (java.sql.PreparedStatement ps) -> {
                            ps.setObject(1, memberId);
                            ps.setObject(2, memberWalletId);
                            ps.setObject(3, walletStatementId);
                            ps.setString(4, email);
                            ps.setString(5, phoneNumber);
                            ps.setString(6, phoneNumber); // p_created_by diisi phone_number
                            return ps.execute();
                        }
                );

                success++;
                if (success % 100 == 0) {
                    log.info("[INJECT-TX] Progress: {} transactions successfully invoked.", success);
                }

            } catch (Exception e) {
                log.error("[INJECT-TX] ❌ Failed to invoke function for memberId={}, email={} -> {}",
                        memberId, email, e.getMessage());
                failed++;
                // Sesuai request Anda sebelumnya: walaupun gagal, tetap lanjut ke baris berikutnya
            }
        }

        log.info("[INJECT-TX] Done. total_processed={}, success={}, failed={}", memberList.size(), success, failed);
    }
}