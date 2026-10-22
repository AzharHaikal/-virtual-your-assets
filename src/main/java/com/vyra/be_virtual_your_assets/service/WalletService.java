package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.WalletEntryType;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.*;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberWallet;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.entity.WalletStatementHistory;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberWalletRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementHistoryRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final MemberWalletRepository memberWalletRepository;
    private final WalletStatementRepository walletStatementRepository;
    private final WalletStatementHistoryRepository walletStatementHistoryRepository;
    private final MemberActivityService memberActivityService;
    private final ValidationService validationService;

    @Transactional
    @NewSpan
    public BaseResponse<CreateWalletResponse> createWallet(CreateWalletRequest request) {
        log.info("[START] walletService.createMemberWallet phoneNumber: {} ", request.getPhoneNumber());

        // One wallet per member / phone number. A second row would make every lookup by
        // phone number fail for that member.
        if (memberWalletRepository.existsByMemberId(request.getMemberId())
                || memberWalletRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            log.warn("Wallet already exists. phoneNumber: {} ", request.getPhoneNumber());
            throw new BusinessException(ErrorConstant.CREATE_WALLET_FAILED);
        }
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_WALLET);

        MemberWallet memberWallet = new MemberWallet();
        memberWallet.setMemberId(request.getMemberId());
        memberWallet.setPhoneNumber(request.getPhoneNumber());
        memberWallet.setCreatedBy(request.getPhoneNumber());
        memberWallet.setCreatedAt(LocalDateTime.now());
        memberWallet.setStatus(ActiveStatus.ACTIVE); // After revamp -> microservice this should be INACTIVE first
        memberWalletRepository.save(memberWallet);

        WalletStatement walletStatement = new WalletStatement();
        walletStatement.setMemberWalletId(memberWallet.getId());
        walletStatement.setPhoneNumber(request.getPhoneNumber());
        walletStatement.setTotalIncome(BigDecimal.ZERO);
        walletStatement.setTotalExpense(BigDecimal.ZERO);
        walletStatement.setBalance(BigDecimal.ZERO);
        walletStatement.setCreatedBy(request.getPhoneNumber());
        walletStatement.setCreatedAt(LocalDateTime.now());
        walletStatementRepository.save(walletStatement);

        CreateWalletResponse response = new CreateWalletResponse();
        response.setMemberId(request.getMemberId());
        response.setPhoneNumber(request.getPhoneNumber());
        response.setBalance(walletStatement.getBalance());

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_CREATE_WALLET);
        log.info("[END] walletService.createMemberWallet successfully phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CREATE_WALLET_SUCCESS.getCode(),
                ErrorConstant.CREATE_WALLET_SUCCESS.getMessage(),
                response
        );
    }

    /**
     * Wallet creation requested through the API. A member may only create a wallet for
     * itself; any mismatch is answered exactly like an unknown account.
     */
    @NewSpan
    public BaseResponse<CreateWalletResponse> createOwnWallet(String authenticatedMemberId, CreateWalletRequest request) {
        Member member = validationService.getMemberById(authenticatedMemberId);
        if (!member.getId().equals(request.getMemberId())
                || !member.getPhoneNumber().equals(request.getPhoneNumber())) {
            log.warn("Rejected wallet creation for another member. memberId: {} ", authenticatedMemberId);
            throw new BusinessException(ErrorConstant.MEMBER_NOT_FOUND);
        }
        return createWallet(request);
    }

    /**
     * Wallet lookup requested through the API: only the caller's own wallet can be read.
     * Someone else's phone number gets the same answer as a number that does not exist.
     */
    @NewSpan
    public BaseResponse<GetMemberWalletResponse> getOwnWallet(String authenticatedMemberId, String phoneNumber) {
        Member member = validationService.getMemberById(authenticatedMemberId);
        if (!member.getPhoneNumber().equals(phoneNumber)) {
            log.warn("Rejected wallet lookup for another member. memberId: {} ", authenticatedMemberId);
            throw new BusinessException(ErrorConstant.MEMBER_NOT_FOUND);
        }
        return getMemberWallet(phoneNumber);
    }

    @NewSpan
    public BaseResponse<GetMemberWalletResponse> getMemberWallet(String phoneNumber) {
        log.info("[START] walletService.getMemberWallet. phoneNumber: {} ", phoneNumber);

        WalletStatement getWallet = walletStatementRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        GetMemberWalletResponse response = new GetMemberWalletResponse();
        response.setPhoneNumber(phoneNumber);
        response.setTotalIncome(getWallet.getTotalIncome());
        response.setTotalExpense(getWallet.getTotalExpense());
        response.setBalance(getWallet.getBalance());

        log.info("[END] walletService.getMemberWallet successfully. phoneNumber: {} ", phoneNumber);

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional
    public void updateMemberWallet(WalletUpdateRequest request) {
        log.info("[START] walletService.updateMemberWallet. searching by old phone: {} ", request.getOldPhoneNumber());

        WalletStatement walletStatement = walletStatementRepository.findByPhoneNumberForUpdate(request.getOldPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        walletStatement.setPhoneNumber(request.getNewPhoneNumber());
        walletStatement.setModifiedBy(request.getNewPhoneNumber());
        walletStatement.setUpdatedAt(LocalDateTime.now());

        MemberWallet memberWallet = memberWalletRepository.findByPhoneNumber(request.getOldPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        memberWallet.setPhoneNumber(request.getNewPhoneNumber());
        memberWallet.setModifiedBy(request.getNewPhoneNumber());
        memberWallet.setUpdatedAt(LocalDateTime.now());

        log.info("[END] walletService.updateMemberWallet successfully phoneNumber: {} ", request.getOldPhoneNumber());
    }

    @Transactional(rollbackFor = Exception.class)
    @NewSpan
    public BaseResponse<TransactionWalletResponse> createTransactionWallet(TransactionWalletRequest request) {
        log.info("[START] walletService.createTransactionWallet phoneNumber: {} ", request.getPhoneNumber());
        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_INSERT_WALLET_STATEMENT);

        // Row lock: serialises concurrent transactions on the same wallet (no lost balance updates).
        WalletStatement wallet = walletStatementRepository.findByPhoneNumberForUpdate(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        BigDecimal amount = request.getAmount();

        WalletStatementHistory history = new WalletStatementHistory();
        history.setWalletStatementId(wallet.getId());
        history.setMemberWalletId(wallet.getMemberWalletId());
        history.setTransactionId(request.getTransactionId());
        history.setPreviousBalance(wallet.getBalance());

        if (TransactionType.INCOME == request.getTransactionType()) {
            history.setIncome(amount);
            history.setExpense(BigDecimal.ZERO);
            history.setCurrentBalance(wallet.getBalance().add(amount));
            wallet.setTotalIncome(wallet.getTotalIncome().add(amount));
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            history.setExpense(amount);
            history.setIncome(BigDecimal.ZERO);
            history.setCurrentBalance(wallet.getBalance().add(amount));
            wallet.setTotalExpense(wallet.getTotalExpense().add(amount));
            wallet.setBalance(wallet.getBalance().add(amount));
        }
        history.setEntryType(WalletEntryType.CREATE);
        history.setCreatedBy(request.getPhoneNumber());
        history.setCreatedAt(LocalDateTime.now());

        walletStatementHistoryRepository.save(history);
        walletStatementRepository.save(wallet);

        TransactionWalletResponse response = new TransactionWalletResponse();
        response.setPhoneNumber(request.getPhoneNumber());
        response.setTotalIncome(wallet.getTotalIncome());
        response.setTotalExpense(wallet.getTotalExpense());
        response.setBalance(wallet.getBalance());

        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_INSERT_WALLET_STATEMENT);
        log.info("[END] walletService.createTransactionWallet successfully. phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode(),
                ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getMessage(),
                response
        );
    }

    /**
     * Applies the difference between a transaction's old and new booking (edit) or removes it (delete).
     * The ledger stays append-only: one history row with the deltas and {@link WalletEntryType#UPDATE} /
     * {@link WalletEntryType#DELETE}. Runs in the caller's transaction (the caller holds the transaction row lock).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionWalletResponse adjustTransactionWallet(AdjustTransactionWalletRequest request) {
        WalletStatement wallet = walletStatementRepository.findByPhoneNumberForUpdate(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        BigDecimal incomeDelta = part(TransactionType.INCOME, request.getNewType(), request.getNewAmount())
                .subtract(part(TransactionType.INCOME, request.getOldType(), request.getOldAmount()));
        BigDecimal expenseDelta = part(TransactionType.EXPENSE, request.getNewType(), request.getNewAmount())
                .subtract(part(TransactionType.EXPENSE, request.getOldType(), request.getOldAmount()));
        BigDecimal previousBalance = wallet.getBalance();

        wallet.setTotalIncome(wallet.getTotalIncome().add(incomeDelta));
        wallet.setTotalExpense(wallet.getTotalExpense().add(expenseDelta));
        wallet.setBalance(previousBalance.add(incomeDelta).add(expenseDelta));
        wallet.setModifiedBy(request.getPhoneNumber());
        wallet.setUpdatedAt(LocalDateTime.now());

        WalletStatementHistory history = new WalletStatementHistory();
        history.setWalletStatementId(wallet.getId());
        history.setMemberWalletId(wallet.getMemberWalletId());
        history.setTransactionId(request.getTransactionId());
        history.setPreviousBalance(previousBalance);
        history.setIncome(incomeDelta);
        history.setExpense(expenseDelta);
        history.setCurrentBalance(wallet.getBalance());
        history.setEntryType(request.getEntryType());
        history.setCreatedBy(request.getPhoneNumber());
        history.setCreatedAt(LocalDateTime.now());

        walletStatementHistoryRepository.save(history);
        walletStatementRepository.save(wallet);

        TransactionWalletResponse response = new TransactionWalletResponse();
        response.setPhoneNumber(request.getPhoneNumber());
        response.setPreviousBalance(previousBalance);
        response.setTotalIncome(wallet.getTotalIncome());
        response.setTotalExpense(wallet.getTotalExpense());
        response.setBalance(wallet.getBalance());
        return response;
    }

    /** The signed amount when the booking is of {@code bucket}'s type, zero otherwise. */
    private static BigDecimal part(TransactionType bucket, TransactionType type, BigDecimal signedAmount) {
        return bucket == type && signedAmount != null ? signedAmount : BigDecimal.ZERO;
    }

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }
}
