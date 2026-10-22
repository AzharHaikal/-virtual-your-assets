package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletRequest;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.repository.WalletStatementHistoryRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database concurrency check for the wallet ledger. Opt-in because it needs a PostgreSQL
 * instance:
 *
 * <pre>
 *   DATABASE_URL=jdbc:postgresql://localhost:5432/scratch DATABASE_USERNAME=... DATABASE_PASSWORD=...
 *   MAIL_USERNAME=x MAIL_PASSWORD=x CRYPTO_SECRET_KEY=0123456789abcdef0123456789abcdef
 *   mvn test -Dtest=WalletConcurrencyIT -Dvyra.it.db=true
 * </pre>
 *
 * Use a scratch database: the application schema is created there and test rows are written.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class WalletConcurrencyIT {

    @Autowired private WalletService walletService;
    @Autowired private WalletStatementRepository walletStatementRepository;
    @Autowired private WalletStatementHistoryRepository walletStatementHistoryRepository;

    @Test
    void concurrentTransactionsOnOneWallet_neverLoseAnUpdate() throws Exception {
        String phone = "628" + (100_000_000 + ThreadLocalRandom.current().nextInt(899_999_999));
        CreateWalletRequest create = new CreateWalletRequest();
        create.setMemberId(UUID.randomUUID().toString());
        create.setPhoneNumber(phone);
        walletService.createWallet(create);

        // The request pool is small (Hikari 10) and every operation also needs a second
        // connection for its activity log, so keep the number of workers modest.
        final int workers = 4;
        final int perWorker = 10;
        final BigDecimal amount = BigDecimal.valueOf(100);

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> results = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            results.add(pool.submit((Callable<Void>) () -> {
                start.await();
                for (int i = 0; i < perWorker; i++) {
                    TransactionWalletRequest request = new TransactionWalletRequest();
                    request.setTransactionId(UUID.randomUUID().toString());
                    request.setPhoneNumber(phone);
                    request.setAmount(amount);
                    request.setTransactionType(TransactionType.INCOME);
                    walletService.createTransactionWallet(request);
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<Void> f : results) f.get(120, TimeUnit.SECONDS);
        pool.shutdown();

        int total = workers * perWorker;
        WalletStatement wallet = walletStatementRepository.findByPhoneNumber(phone).orElseThrow();

        assertThat(wallet.getBalance()).isEqualByComparingTo(amount.multiply(BigDecimal.valueOf(total)));
        assertThat(wallet.getTotalIncome()).isEqualByComparingTo(amount.multiply(BigDecimal.valueOf(total)));
        long historyRows = walletStatementHistoryRepository.findAll().stream()
                .filter(h -> h.getWalletStatementId().equals(wallet.getId()))
                .count();
        assertThat(historyRows).isEqualTo(total);
    }
}
