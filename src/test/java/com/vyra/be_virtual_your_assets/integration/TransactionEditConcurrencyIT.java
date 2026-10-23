package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parallel edits, deletes (including the same delete twice) and new transactions on one wallet
 * against a real PostgreSQL: the balance and totals always match the transactions that are left.
 * Opt-in, see {@link SchemaMigrationIT} for how to run it ({@code -Dvyra.it.db=true}).
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class TransactionEditConcurrencyIT {

    @Autowired private MemberRepository memberRepository;
    @Autowired private WalletService walletService;
    @Autowired private TransactionService transactionService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private WalletStatementRepository walletStatementRepository;

    @Test
    void parallelEditsAndDeletes_keepTheWalletEqualToTheRemainingTransactions() throws Exception {
        Member member = ItMembers.create(memberRepository, walletService);
        String memberId = member.getId();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(create(memberId, TransactionType.INCOME, TransactionCategory.SALARY, 100));
        }
        List<String> toEdit = ids.subList(0, 5);
        List<String> toDelete = ids.subList(5, 10);

        List<Callable<?>> tasks = List.of(
                () -> {
                    for (String id : toEdit) {
                        UpdateTransactionRequest edit = new UpdateTransactionRequest();
                        edit.setAmount(BigDecimal.valueOf(300));
                        transactionService.updateOwnTransaction(memberId, id, edit);
                    }
                    return null;
                },
                // The same deletes twice at the same time: each one may only be booked once.
                () -> {
                    for (String id : toDelete) transactionService.deleteOwnTransaction(memberId, id);
                    return null;
                },
                () -> {
                    for (String id : toDelete) transactionService.deleteOwnTransaction(memberId, id);
                    return null;
                },
                () -> {
                    for (int i = 0; i < 5; i++) create(memberId, TransactionType.EXPENSE, TransactionCategory.FOOD_AND_BEVERAGE, 50);
                    return null;
                });

        assertThat(ItMembers.runTogether(tasks)).containsOnlyNulls();

        WalletStatement wallet = walletStatementRepository.findByPhoneNumber(member.getPhoneNumber()).orElseThrow();
        assertThat(wallet.getTotalIncome()).isEqualByComparingTo("1500");
        // The wallet keeps expenses signed, like the transaction amounts.
        assertThat(wallet.getTotalExpense()).isEqualByComparingTo("-250");
        assertThat(wallet.getBalance()).isEqualByComparingTo("1250");

        // The same numbers straight from the transactions that are left.
        BigDecimal remaining = transactionRepository.findAll().stream()
                .filter(t -> memberId.equals(t.getMemberId()))
                .filter(t -> t.getDeletedAt() == null && t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(wallet.getBalance()).isEqualByComparingTo(remaining);
    }

    private String create(String memberId, TransactionType type, TransactionCategory category, long amount) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(type);
        request.setCategory(category);
        request.setAmount(BigDecimal.valueOf(amount));
        return transactionService.createTransaction(memberId, request).getData().getTransactionId();
    }
}
