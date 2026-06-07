package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.MemberStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.*;
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
        log.info("[START] walletService.createMemberWallet phoneNumber: {} ", request.getPhoneNumber());
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_WALLET);

        MemberWallet memberWallet = new MemberWallet();
        memberWallet.setMemberId(request.getMemberId());
        memberWallet.setPhoneNumber(request.getPhoneNumber());
        memberWallet.setCreatedBy(request.getPhoneNumber());
        memberWallet.setCreatedAt(LocalDateTime.now());
        memberWallet.setStatus(MemberStatus.ACTIVE); // After revamp -> microservice this should be INACTIVE first
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

        WalletStatement walletStatement = walletStatementRepository.findByPhoneNumber(request.getOldPhoneNumber())
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

        WalletStatement wallet = walletStatementRepository.findByPhoneNumber(request.getPhoneNumber())
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

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }
}
