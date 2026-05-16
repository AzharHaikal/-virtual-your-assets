package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.virtual_your_assets.dto.wallet.TransactionWalletRequest;
import com.vyra.virtual_your_assets.dto.wallet.TransactionWalletResponse;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.Transaction;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.TransactionRepository;
import com.vyra.virtual_your_assets.util.TransactionUtil;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
    public BaseResponse<CreateTransactionResponse> createTransaction(String memberId, CreateTransactionRequest request) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.createTransaction phoneNumber: {} ", member.getPhoneNumber());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_TRANSACTION);

        String referenceNumber = TransactionUtil.generateReferenceNumber();

        Transaction transaction = buildTransaction(request, member, referenceNumber);
        transactionRepository.save(transaction);

        // Wallet process
        TransactionWalletResponse walletData = processWallet(transaction, request, member.getPhoneNumber());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        CreateTransactionResponse response = buildResponse(transaction, walletData);

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_GET_MEMBER_DETAIL);
        log.info("[END] transactionService.createTransaction successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional(readOnly = true)
    public BaseResponse<Page<TransactionHistoryResponse>> getTransactionHistory(
            String startDate, String endDate, String type, int page, int size, String memberId
    ) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.getTransactionHistory phoneNumber: {} ", member.getPhoneNumber());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GET_TRANSACTION_HISTORY);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        Page<Transaction> transactions = transactionRepository.findTransactionHistory(memberId, start, end, type, pageable);
        Page<TransactionHistoryResponse> responses = transactions.map(this::mapToHistoryResponse);

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_GET_TRANSACTION_HISTORY);
        log.info("[END] transactionService.getTransactionHistory successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getCode(),
                ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getMessage(),
                responses
        );
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
                .createdBy(member.getPhoneNumber())
                .createdAt(LocalDateTime.now())
                .status(TransactionStatus.INQUIRY)
                .build();
    }

    private TransactionWalletResponse processWallet(Transaction transaction, CreateTransactionRequest request, String phoneNumber) {
        BaseResponse<TransactionWalletResponse> response;
        try {
            log.info("Insert transaction into wallet. phoneNumber: {} ", phoneNumber);
            TransactionWalletRequest walletRequest = new TransactionWalletRequest();
            walletRequest.setTransactionId(transaction.getId());
            walletRequest.setPhoneNumber(phoneNumber);
            walletRequest.setAmount(request.getAmount());
            walletRequest.setCategory(request.getCategory());

            response = walletService.createTransactionWallet(walletRequest);
            if (!ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode().equals(response.getResponseStatus())) {
                log.info("Failed when insert transaction in wallet service. phoneNumber: {}", phoneNumber);
                throw new BusinessException(ErrorConstant.CREATE_TRANSACTION_WALLET_FAILED);
            }

        } catch (BusinessException e) {
            log.error("[ERROR] create transaction {} encountered an exception: {}", phoneNumber, e.getMessage(), e);
            createMemberActivity(phoneNumber, MemberActivityEvent.FAILED_CREATE_TRANSACTION);
            throw new BusinessException(ErrorConstant.BAD_REQUEST);

        } catch (Exception e) {
            log.error("[ERROR] Unexpected error during create transaction {}, message: {}", phoneNumber, e.getMessage(), e);
            createMemberActivity(phoneNumber, MemberActivityEvent.FAILED_CREATE_TRANSACTION);
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

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }

    private TransactionHistoryResponse mapToHistoryResponse(Transaction transaction) {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        response.setTransactionId(transaction.getId());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setType(transaction.getType());
        response.setCategory(transaction.getCategory());
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setTransactionDesc(transaction.getTransactionDesc());

        return response;
    }

}
