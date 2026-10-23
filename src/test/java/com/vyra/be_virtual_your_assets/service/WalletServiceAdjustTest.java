package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.WalletEntryType;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.wallet.AdjustTransactionWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletResponse;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.entity.WalletStatementHistory;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberWalletRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementHistoryRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Wallet corrections for edited / deleted transactions (amounts signed: expense negative). */
@ExtendWith(MockitoExtension.class)
class WalletServiceAdjustTest {

    private static final String PHONE = "628123456789";

    @Mock private MemberWalletRepository memberWalletRepository;
    @Mock private WalletStatementRepository walletStatementRepository;
    @Mock private WalletStatementHistoryRepository walletStatementHistoryRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private ValidationService validationService;

    @InjectMocks
    private WalletService walletService;

    private WalletStatement wallet;

    @BeforeEach
    void setUp() {
        // income 1.000.000, expense -250.000 -> balance 750.000
        wallet = new WalletStatement();
        wallet.setId("ws-1");
        wallet.setMemberWalletId("mw-1");
        wallet.setPhoneNumber(PHONE);
        wallet.setTotalIncome(new BigDecimal("1000000"));
        wallet.setTotalExpense(new BigDecimal("-250000"));
        wallet.setBalance(new BigDecimal("750000"));
        when(walletStatementRepository.findByPhoneNumberForUpdate(PHONE)).thenReturn(Optional.of(wallet));
    }

    @Test
    void expenseAmountChanged_shouldMoveBalanceByTheDifference() {
        TransactionWalletResponse result = walletService.adjustTransactionWallet(request(
                TransactionType.EXPENSE, "-50000", TransactionType.EXPENSE, "-75000", WalletEntryType.UPDATE));

        assertThat(result.getBalance()).isEqualByComparingTo("725000");
        assertThat(wallet.getTotalExpense()).isEqualByComparingTo("-275000");
        assertThat(wallet.getTotalIncome()).isEqualByComparingTo("1000000");

        WalletStatementHistory history = savedHistory();
        assertThat(history.getEntryType()).isEqualTo(WalletEntryType.UPDATE);
        assertThat(history.getPreviousBalance()).isEqualByComparingTo("750000");
        assertThat(history.getCurrentBalance()).isEqualByComparingTo("725000");
        assertThat(history.getExpense()).isEqualByComparingTo("-25000");
        assertThat(history.getIncome()).isEqualByComparingTo("0");
        assertThat(history.getTransactionId()).isEqualTo("trx-1");
    }

    @Test
    void expenseChangedToIncome_shouldMoveTotalsBetweenBuckets() {
        walletService.adjustTransactionWallet(request(
                TransactionType.EXPENSE, "-50000", TransactionType.INCOME, "50000", WalletEntryType.UPDATE));

        assertThat(wallet.getBalance()).isEqualByComparingTo("850000");
        assertThat(wallet.getTotalExpense()).isEqualByComparingTo("-200000");
        assertThat(wallet.getTotalIncome()).isEqualByComparingTo("1050000");
    }

    @Test
    void delete_shouldRemoveTheBooking() {
        walletService.adjustTransactionWallet(request(
                TransactionType.INCOME, "1000000", null, "0", WalletEntryType.DELETE));

        assertThat(wallet.getBalance()).isEqualByComparingTo("-250000");
        assertThat(wallet.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(savedHistory().getEntryType()).isEqualTo(WalletEntryType.DELETE);
        verify(walletStatementRepository).save(wallet);
    }

    @Test
    void unknownWallet_shouldThrowAndWriteNothing() {
        when(walletStatementRepository.findByPhoneNumberForUpdate(PHONE)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> walletService.adjustTransactionWallet(request(
                TransactionType.INCOME, "1", TransactionType.INCOME, "2", WalletEntryType.UPDATE)));

        verify(walletStatementHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static AdjustTransactionWalletRequest request(TransactionType oldType, String oldAmount,
                                                          TransactionType newType, String newAmount,
                                                          WalletEntryType entryType) {
        return AdjustTransactionWalletRequest.builder()
                .phoneNumber(PHONE)
                .transactionId("trx-1")
                .oldType(oldType).oldAmount(new BigDecimal(oldAmount))
                .newType(newType).newAmount(new BigDecimal(newAmount))
                .entryType(entryType)
                .build();
    }

    private WalletStatementHistory savedHistory() {
        ArgumentCaptor<WalletStatementHistory> captor = ArgumentCaptor.forClass(WalletStatementHistory.class);
        verify(walletStatementHistoryRepository).save(captor.capture());
        return captor.getValue();
    }
}
