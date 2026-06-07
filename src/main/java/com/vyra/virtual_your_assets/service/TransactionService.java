package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.chart.ChartItemResponse;
import com.vyra.virtual_your_assets.dto.chart.GetChartResponse;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.virtual_your_assets.dto.transaction.TransactionUpdateRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final MemberActivityService memberActivityService;
    private final ValidationService validationService;
    private final WalletService walletService;

    @NewSpan
    public BaseResponse<GetChartResponse> getChart(String memberId, String period) {
        String normalizedPeriod = period == null ? "ALL" : period.toUpperCase();

        List<Object[]> results = switch (normalizedPeriod) {
            case "1W" -> transactionRepository.getChartDaily(memberId, LocalDateTime.now().minusWeeks(1));
            case "1M" -> transactionRepository.getChartDaily(memberId, LocalDateTime.now().minusMonths(1));
            case "3M" -> transactionRepository.getChartWeekly(memberId, LocalDateTime.now().minusMonths(3));
            case "1Y" -> transactionRepository.getChartMonthly(memberId, LocalDateTime.now().minusYears(1));
            default -> transactionRepository.getChartMonthlyAll(memberId);
        };

        List<ChartItemResponse> chart = buildChartWithEmptyBucket(normalizedPeriod, results);

        GetChartResponse response = new GetChartResponse();
        response.setChart(chart);

        return new BaseResponse<>(
                ErrorConstant.GET_CHART_SUCCESS.getCode(),
                ErrorConstant.GET_CHART_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional(readOnly = true)
    public BaseResponse<List<TransactionHistoryResponse>> getTopTransactionHistory(String memberId) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.getTopTransactionHistory phoneNumber: {} ", member.getPhoneNumber());

        List<Transaction> transactions = transactionRepository.findTransactionHistory(memberId);
        List<TransactionHistoryResponse> responses = transactions.stream().map(this::mapToHistoryResponse).toList();

        log.info("Response : {}", responses);
        log.info("[END] transactionService.getTopTransactionHistory successfully phoneNumber: {} ", member.getPhoneNumber());
        return new BaseResponse<>(
                ErrorConstant.GET_TOP_TRANSACTION_HISTORY_SUCCESS.getCode(),
                ErrorConstant.GET_TOP_TRANSACTION_HISTORY_SUCCESS.getMessage(),
                responses
        );
    }

    @NewSpan
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<CreateTransactionResponse> createTransaction(String memberId, CreateTransactionRequest request) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.createTransaction phoneNumber: {} ", member.getPhoneNumber());
        String referenceNumber = TransactionUtil.generateReferenceNumber();

        BigDecimal amount = request.getAmount();
        if (TransactionType.EXPENSE.equals(request.getType())) {
            request.setAmount(amount.negate());
        }

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_TRANSACTION);

        Transaction transaction = createTransaction(request, member, referenceNumber);
        transactionRepository.save(transaction);

        // Wallet process
        TransactionWalletResponse walletData = processWallet(transaction, request, member.getPhoneNumber());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        CreateTransactionResponse response = createTransactionResponse(transaction, walletData);

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_CREATE_TRANSACTION);
        log.info("[END] transactionService.createTransaction successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CREATE_TRANSACTION_SUCCESS.getCode(),
                ErrorConstant.CREATE_TRANSACTION_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional(readOnly = true)
    public BaseResponse<Page<TransactionHistoryResponse>> getTransactionHistory(
            String startDate, String endDate, TransactionType type, int page, int size, String memberId
    ) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.getTransactionHistory phoneNumber: {} ", member.getPhoneNumber());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        Page<Transaction> transactions = transactionRepository.findTransactionHistory(memberId, start, end, type, pageable);
        Page<TransactionHistoryResponse> responses = transactions.map(this::mapToHistoryResponse);

        log.info("[END] transactionService.getTransactionHistory successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getCode(),
                ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getMessage(),
                responses
        );
    }

    private Transaction createTransaction(CreateTransactionRequest request, Member member, String ref) {
        return Transaction.builder()
                .memberId(member.getId())
                .userPhoneNumber(member.getPhoneNumber())
                .userEmail(member.getEmail())
                .transactionCategory(request.getCategory().name())
                .transactionType(request.getType())
                .amount(request.getAmount())
                .transactionDesc(request.getTransactionDesc())
                .referenceNumber(ref)
                .transactionDate(request.getTransactionDate())
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
            walletRequest.setTransactionType(request.getType());

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

    @NewSpan
    @Transactional
    public void updateTransaction(TransactionUpdateRequest request) {
        log.info("[START] transactionService.updateTransaction searching by old phone: {} ", request.getOldPhoneNumber());
        List<Transaction> transactions = transactionRepository.findAllByUserPhoneNumber(request.getOldPhoneNumber());
        if (!transactions.isEmpty()) {
            for (Transaction transaction : transactions) {
                transaction.setUserPhoneNumber(request.getNewPhoneNumber());
                transaction.setUserEmail(request.getEmail());
                transaction.setModifiedBy(request.getNewPhoneNumber());
                transaction.setUpdatedAt(LocalDateTime.now());
            }
            log.info("[END] transactionService.updateTransaction successfully updated {} rows", transactions.size());
        }
    }

    private CreateTransactionResponse createTransactionResponse(Transaction trx, TransactionWalletResponse wallet) {
        CreateTransactionResponse response = new CreateTransactionResponse();
        response.setTransactionId(trx.getId());
        response.setPhoneNumber(trx.getUserPhoneNumber());
        response.setEmail(trx.getUserEmail());
        response.setCategory(trx.getTransactionCategory());
        response.setTransactionType(trx.getTransactionType());
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
        response.setCategory(transaction.getTransactionCategory());
        response.setType(transaction.getTransactionType());
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setTransactionDesc(transaction.getTransactionDesc());
        response.setTransactionDate(transaction.getTransactionDate());
        return response;
    }

    private List<ChartItemResponse> buildChartWithEmptyBucket(String period, List<Object[]> results) {
        return switch (period) {
            case "1W" -> buildDailyChart(LocalDate.now().minusDays(6), LocalDate.now(), results);
            case "1M" -> buildDailyChart(LocalDate.now().minusMonths(1), LocalDate.now(), results);
            case "3M" -> buildWeeklyChart(results);
            default -> buildMonthlyChart(results);
        };
    }

    private List<ChartItemResponse> buildDailyChart(LocalDate start, LocalDate end, List<Object[]> results) {
        Map<String, ChartItemResponse> data = results.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> new ChartItemResponse(
                                row[0].toString(),
                                (BigDecimal) row[1],
                                (BigDecimal) row[2]
                        )
                ));
        List<ChartItemResponse> chart = new ArrayList<>();

        for (LocalDate date = start;
             !date.isAfter(end);
             date = date.plusDays(1)) {

            String label = date.toString();
            chart.add(data.getOrDefault(label, new ChartItemResponse(label, BigDecimal.ZERO, BigDecimal.ZERO)));
        }

        return chart;
    }

    private List<ChartItemResponse> buildWeeklyChart(List<Object[]> results) {
        return results.stream()
                .map(row -> new ChartItemResponse(
                        row[0].toString(),
                        (BigDecimal) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    private List<ChartItemResponse> buildMonthlyChart(List<Object[]> results) {
        return results.stream()
                .map(row -> new ChartItemResponse(
                        row[0].toString(),
                        (BigDecimal) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }
}
