package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.wallet.*;
import com.vyra.virtual_your_assets.entity.MemberWallet;
import com.vyra.virtual_your_assets.entity.WalletStatement;
import com.vyra.virtual_your_assets.entity.WalletStatementHistory;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberWalletRepository;
import com.vyra.virtual_your_assets.repository.WalletStatementHistoryRepository;
import com.vyra.virtual_your_assets.repository.WalletStatementRepository;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    @Transactional
    @NewSpan
    public BaseResponse<CreateWalletResponse> createWallet(CreateWalletRequest request) {
        log.info("[START] walletService.createMemberWallet. phoneNumber: {} ", request.getPhoneNumber());
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_WALLET);

        MemberWallet memberWallet = new MemberWallet();
        memberWallet.setMemberId(request.getMemberId());
        memberWallet.setPhoneNumber(request.getPhoneNumber());
        memberWallet.setCreatedBy(request.getPhoneNumber());
        memberWallet.setCreatedAt(LocalDateTime.now());
        memberWalletRepository.save(memberWallet);

        WalletStatement walletStatement = new WalletStatement();
        walletStatement.setMemberWalletId(memberWallet.getId());
        walletStatement.setPhoneNumber(request.getPhoneNumber());
        walletStatement.setTotalCredit(BigDecimal.ZERO);
        walletStatement.setTotalDebit(BigDecimal.ZERO);
        walletStatement.setBalance(BigDecimal.ZERO);
        walletStatement.setCreatedBy(request.getPhoneNumber());
        walletStatement.setCreatedAt(LocalDateTime.now());
        walletStatementRepository.save(walletStatement);

        CreateWalletResponse response = new CreateWalletResponse();
        response.setMemberId(request.getMemberId());
        response.setPhoneNumber(request.getPhoneNumber());
        response.setBalance(walletStatement.getBalance());

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_CREATE_WALLET);
        log.info("[END] walletService.createMemberWallet successfully. phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CREATE_WALLET_SUCCESS.getCode(),
                ErrorConstant.CREATE_WALLET_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    public BaseResponse<GetMemberWalletResponse> getMemberWallet(String phoneNumber) {
        log.info("[START] walletService.getMemberWallet. phoneNumber: {} ", phoneNumber);
        createMemberActivity(phoneNumber, MemberActivityEvent.ATTEMPT_GET_MEMBER_WALLET);

        WalletStatement getWallet = walletStatementRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        GetMemberWalletResponse response = new GetMemberWalletResponse();
        response.setPhoneNumber(phoneNumber);
        response.setTotalCredit(getWallet.getTotalCredit());
        response.setTotalDebit(getWallet.getTotalDebit());
        response.setBalance(getWallet.getBalance());

        createMemberActivity(phoneNumber, MemberActivityEvent.SUCCESS_GET_MEMBER_WALLET);
        log.info("[END] walletService.getMemberWallet successfully. phoneNumber: {} ", phoneNumber);

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @NewSpan
    public BaseResponse<TransactionWalletResponse> createTransactionWallet(TransactionWalletRequest request) {
        log.info("[START] walletService.createTransactionWallet. phoneNumber: {} ", request.getPhoneNumber());
        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_INSERT_WALLET_STATEMENT);

        WalletStatement wallet = walletStatementRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        BigDecimal amount = request.getAmount();

        WalletStatementHistory history = new WalletStatementHistory();
        history.setWalletStatementId(wallet.getId());
        history.setMemberWalletId(wallet.getMemberWalletId());
        history.setTransactionId(request.getTransactionId());
        history.setPreviousBalance(wallet.getBalance());

        if (TransactionCategory.CREDIT == request.getCategory()) {
            history.setCredit(amount);
            wallet.setTotalCredit(wallet.getTotalCredit().add(amount));
            wallet.setBalance(wallet.getBalance().add(amount));
        } else {
            history.setDebit(amount);
            wallet.setTotalDebit(wallet.getTotalDebit().add(amount));
            wallet.setBalance(wallet.getBalance().subtract(amount));
        }

        walletStatementHistoryRepository.save(history);
        walletStatementRepository.save(wallet);

        TransactionWalletResponse response = new TransactionWalletResponse();
        response.setPhoneNumber(request.getPhoneNumber());
        response.setTotalCredit(wallet.getTotalCredit());
        response.setTotalDebit(wallet.getTotalDebit());
        response.setBalance(wallet.getBalance());

        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_INSERT_WALLET_STATEMENT);
        log.info("[END] walletService.createTransactionWallet successfully. phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode(),
                ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getMessage(),
                response
        );
    }

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }
}
