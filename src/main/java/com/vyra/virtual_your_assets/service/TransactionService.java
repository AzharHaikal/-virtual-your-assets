package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.virtual_your_assets.dto.wallet.TransactionWalletRequest;
import com.vyra.virtual_your_assets.dto.wallet.TransactionWalletResponse;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.Transaction;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.TransactionRepository;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final MemberActivityService memberActivityService;
    private final ValidationService validationService;
    private final WalletService walletService;

    @Transactional(rollbackFor = Exception.class)
    @NewSpan
    public BaseResponse<CreateTransactionResponse> createTransaction(CreateTransactionRequest request) {
        log.info("[START] transactionService.createTransaction phoneNumber: {} ", request.getPhoneNumber());
        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_TRANSACTION);

        Member member = validationService.getMemberByPhoneNumber(request.getPhoneNumber());

        String referenceNumber = generateReferenceNumber();

        Transaction transaction = buildTransaction(request, member, referenceNumber);
        transactionRepository.save(transaction);

        // Wallet process
        TransactionWalletResponse walletData = processWallet(transaction, request);

        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        CreateTransactionResponse response = buildResponse(transaction, walletData);

        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_GET_MEMBER_DETAIL);
        log.info("[END] transactionService.createTransaction successfully phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_SUCCESS.getMessage(),
                response
        );
    }

    private String generateReferenceNumber() {
        return "VYRA" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private Transaction buildTransaction(CreateTransactionRequest request, Member member, String ref) {
        return Transaction.builder()
                .userPhoneNumber(member.getPhoneNumber())
                .userEmail(member.getEmail())
                .category(request.getCategory())
                .type(TransactionType.FINANCE) // Note: VYRA 2.0 new feature crypto, stock etc
                .amount(request.getAmount())
                .transactionDesc(request.getTransactionDesc())
                .referenceNumber(ref)
                .transactionDate(LocalDateTime.now())
                .createdBy(request.getPhoneNumber())
                .createdAt(LocalDateTime.now())
                .status(TransactionStatus.INQUIRY)
                .build();
    }

    private TransactionWalletResponse processWallet(Transaction transaction, CreateTransactionRequest request) {
        BaseResponse<TransactionWalletResponse> response;
        try {
            log.info("Insert transaction into wallet. phoneNumber: {} ", request.getPhoneNumber());
            TransactionWalletRequest walletRequest = new TransactionWalletRequest();
            walletRequest.setTransactionId(transaction.getTransactionId());
            walletRequest.setPhoneNumber(request.getPhoneNumber());
            walletRequest.setAmount(request.getAmount());
            walletRequest.setCategory(request.getCategory());

            response = walletService.createTransactionWallet(walletRequest);
            if (!ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode().equals(response.getResponseStatus())) {
                log.info("Failed when insert transaction in wallet service. phoneNumber: {}", request.getPhoneNumber());
                throw new BusinessException(ErrorConstant.CREATE_TRANSACTION_WALLET_FAILED);
            }

        } catch (BusinessException e) {
            log.error("[ERROR] create transaction {} encountered an exception: {}", request.getPhoneNumber(), e.getMessage(), e);
            createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_CREATE_TRANSACTION);
            throw new BusinessException(ErrorConstant.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[ERROR] Unexpected error during create transaction {}, message: {}", request.getPhoneNumber(), e.getMessage(), e);
            createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_CREATE_TRANSACTION);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        return response.getData();
    }

    private CreateTransactionResponse buildResponse(Transaction trx, TransactionWalletResponse wallet) {
        CreateTransactionResponse response = new CreateTransactionResponse();
        response.setPhoneNumber(trx.getUserPhoneNumber());
        response.setEmail(trx.getUserEmail());
        response.setCategory(trx.getCategory());
        response.setTransactionType(trx.getType());
        response.setAmount(trx.getAmount());
        response.setReferenceNumber(trx.getReferenceNumber());
        response.setTransactionDesc(trx.getTransactionDesc());
        response.setBalance(wallet.getBalance());
        return response;
    }

    private void createMemberActivity(Object object, String activityEvent) {
        memberActivityService.createMemberActivity(object.toString(), activityEvent);
    }

}
