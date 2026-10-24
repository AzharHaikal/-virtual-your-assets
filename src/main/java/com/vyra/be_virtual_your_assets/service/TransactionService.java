package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.WalletEntryType;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.chart.ChartItemResponse;
import com.vyra.be_virtual_your_assets.dto.chart.GetChartResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionMutationResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.AdjustTransactionWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletResponse;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.event.TransactionChangedEvent;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.util.TransactionUtil;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final MemberActivityService memberActivityService;
    private final ValidationService validationService;
    private final WalletService walletService;
    private final TimeService timeService;
    private final CategoryService categoryService;
    private final ActivationService activationService;
    private final ApplicationEventPublisher eventPublisher;

    @NewSpan
    public BaseResponse<GetChartResponse> getChart(String memberId, String period) {
        String normalizedPeriod = period == null ? "1W" : period.toUpperCase();
        // Buckets are the member's own days (transaction.local_date), so "today" is in their zone.
        LocalDate today = timeService.today(timeService.zoneOf(validationService.getMemberById(memberId)));

        List<Object[]> results = switch (normalizedPeriod) {
            case "1M" -> transactionRepository.getChartDaily(memberId, today.minusMonths(1));
            case "3M" -> transactionRepository.getChartWeekly(memberId, today.minusMonths(3));
            case "1Y" -> transactionRepository.getChartMonthly(memberId, today.minusYears(1));
            case "ALL" -> transactionRepository.getChartMonthlyAll(memberId);
            default   -> transactionRepository.getChartDaily(memberId, today.minusDays(6));
        };

        List<ChartItemResponse> chart = buildChartWithEmptyBucket(normalizedPeriod, results, today);

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
        Map<String, FinanceCategory> categories = categoryService.byIds(
                transactions.stream().map(Transaction::getCategoryId).toList());
        List<TransactionHistoryResponse> responses = transactions.stream()
                .map(t -> mapToHistoryResponse(t, categories))
                .toList();
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
        activationService.record(memberId, ActivationEvent.FIRST_TRANSACTION);
        publishChange(memberId, transaction.getCategoryId(), transaction.getLocalDate(), null, null);

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

        // Dates are the member's own days (transaction.local_date), both inclusive.
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        Page<Transaction> transactions = transactionRepository.findTransactionHistory(memberId, start, end, type, pageable);
        Map<String, FinanceCategory> categories = categoryService.byIds(
                transactions.getContent().stream().map(Transaction::getCategoryId).toList());
        Page<TransactionHistoryResponse> responses = transactions.map(t -> mapToHistoryResponse(t, categories));

        log.info("[END] transactionService.getTransactionHistory successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getCode(),
                ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getMessage(),
                responses
        );
    }

    private Transaction createTransaction(CreateTransactionRequest request, Member member, String ref) {
        ZoneId zone = timeService.zoneOf(member);
        LocalDateTime occurredAt = timeService.parseClientDateTime(request.getTransactionDate(), zone);
        FinanceCategory category = categoryService.resolveForTransaction(
                member.getId(), request.getType(), request.getCategoryId(), request.getCategory());
        return Transaction.builder()
                .memberId(member.getId())
                .userPhoneNumber(member.getPhoneNumber())
                .userEmail(member.getEmail())
                .transactionCategory(category.legacyCode())
                .categoryId(category.getId())
                .paymentMethod(request.getPaymentMethod())
                .transactionType(request.getType())
                .amount(request.getAmount())
                // Optional in the request but NOT NULL in the table.
                .transactionDesc(StringUtils.trimToEmpty(request.getTransactionDesc()))
                .referenceNumber(ref)
                .transactionDate(occurredAt)
                .localDate(timeService.toLocalDate(occurredAt, zone))
                .createdBy(member.getPhoneNumber())
                .createdAt(timeService.nowUtc())
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
            log.warn("create transaction {} encountered an exception: {}", phoneNumber, e.getMessage());
            createMemberActivity(phoneNumber, MemberActivityEvent.FAILED_CREATE_TRANSACTION);
            throw new BusinessException(ErrorConstant.BAD_REQUEST);

        } catch (Exception e) {
            log.error("Unexpected error during create transaction {}, message: {}", phoneNumber, e.getMessage(), e);
            createMemberActivity(phoneNumber, MemberActivityEvent.FAILED_CREATE_TRANSACTION);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        return response.getData();
    }

    /**
     * Edits the member's own transaction and corrects the wallet by the difference (PATCH: null keeps a value).
     * Only SUCCESS transactions can be edited; this is bookkeeping, not real money, so any field may change.
     */
    @NewSpan
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<TransactionMutationResponse> updateOwnTransaction(String memberId, String transactionId,
                                                                          UpdateTransactionRequest request) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.updateOwnTransaction phoneNumber: {} ", member.getPhoneNumber());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_UPDATE_TRANSACTION);

        Transaction transaction = transactionRepository.findOwnedForUpdate(transactionId, memberId)
                .filter(t -> t.getDeletedAt() == null && t.getStatus() != TransactionStatus.DELETED)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.TRANSACTION_NOT_FOUND));
        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new BadRequestException(ErrorConstant.TRANSACTION_NOT_EDITABLE);
        }

        ZoneId zone = timeService.zoneOf(member);
        TransactionType oldType = transaction.getTransactionType();
        BigDecimal oldAmount = transaction.getAmount();
        String oldCategoryId = transaction.getCategoryId();
        LocalDate oldLocalDate = transaction.getLocalDate();
        TransactionType newType = request.getType() != null ? request.getType() : oldType;
        BigDecimal absolute = request.getAmount() != null ? request.getAmount() : oldAmount.abs();
        BigDecimal newAmount = newType == TransactionType.EXPENSE ? absolute.negate() : absolute;

        if (request.getTransactionDate() != null) {
            LocalDateTime occurredAt = timeService.parseClientDateTime(request.getTransactionDate(), zone);
            transaction.setTransactionDate(occurredAt);
            transaction.setLocalDate(timeService.toLocalDate(occurredAt, zone));
        }
        if (request.getCategoryId() != null || request.getCategory() != null || newType != oldType) {
            FinanceCategory category = categoryService.resolveForTransaction(
                    memberId, newType, request.getCategoryId(), request.getCategory());
            transaction.setCategoryId(category.getId());
            transaction.setTransactionCategory(category.legacyCode());
        }
        if (request.getTransactionDesc() != null) {
            transaction.setTransactionDesc(request.getTransactionDesc().trim());
        }
        if (request.getPaymentMethod() != null) {
            transaction.setPaymentMethod(request.getPaymentMethod());
        }
        transaction.setTransactionType(newType);
        transaction.setAmount(newAmount);
        transaction.setModifiedBy(member.getPhoneNumber());
        transaction.setUpdatedAt(timeService.nowUtc());

        TransactionWalletResponse wallet = walletService.adjustTransactionWallet(AdjustTransactionWalletRequest.builder()
                .phoneNumber(member.getPhoneNumber())
                .transactionId(transaction.getId())
                .oldType(oldType).oldAmount(oldAmount)
                .newType(newType).newAmount(newAmount)
                .entryType(WalletEntryType.UPDATE)
                .build());
        transactionRepository.save(transaction);
        publishChange(memberId, transaction.getCategoryId(), transaction.getLocalDate(), oldCategoryId, oldLocalDate);

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_UPDATE_TRANSACTION);
        log.info("[END] transactionService.updateOwnTransaction successfully phoneNumber: {} ", member.getPhoneNumber());
        return new BaseResponse<>(
                ErrorConstant.UPDATE_TRANSACTION_SUCCESS.getCode(),
                ErrorConstant.UPDATE_TRANSACTION_SUCCESS.getMessage(),
                toMutationResponse(transaction, wallet.getBalance(), wallet.getTotalIncome(), wallet.getTotalExpense())
        );
    }

    /**
     * Soft-deletes the member's own transaction (status DELETED + deleted_at) and removes it from the wallet.
     * Idempotent: deleting an already deleted transaction changes nothing.
     */
    @NewSpan
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<TransactionMutationResponse> deleteOwnTransaction(String memberId, String transactionId) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] transactionService.deleteOwnTransaction phoneNumber: {} ", member.getPhoneNumber());

        Transaction transaction = transactionRepository.findOwnedForUpdate(transactionId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.TRANSACTION_NOT_FOUND));

        boolean alreadyDeleted = transaction.getDeletedAt() != null || transaction.getStatus() == TransactionStatus.DELETED;
        if (!alreadyDeleted) {
            // Only SUCCESS transactions were booked in the wallet (INQUIRY / FAILED never were).
            if (transaction.getStatus() == TransactionStatus.SUCCESS) {
                walletService.adjustTransactionWallet(AdjustTransactionWalletRequest.builder()
                        .phoneNumber(member.getPhoneNumber())
                        .transactionId(transaction.getId())
                        .oldType(transaction.getTransactionType()).oldAmount(transaction.getAmount())
                        .newType(null).newAmount(BigDecimal.ZERO)
                        .entryType(WalletEntryType.DELETE)
                        .build());
            }
            LocalDateTime now = timeService.nowUtc();
            transaction.setStatus(TransactionStatus.DELETED);
            transaction.setDeletedAt(now);
            transaction.setUpdatedAt(now);
            transaction.setModifiedBy(member.getPhoneNumber());
            transactionRepository.save(transaction);
            publishChange(memberId, transaction.getCategoryId(), transaction.getLocalDate(), null, null);
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_DELETE_TRANSACTION);
        }

        GetMemberWalletResponse wallet = walletService.getMemberWallet(member.getPhoneNumber()).getData();
        log.info("[END] transactionService.deleteOwnTransaction phoneNumber: {} alreadyDeleted: {}",
                member.getPhoneNumber(), alreadyDeleted);
        return new BaseResponse<>(
                ErrorConstant.DELETE_TRANSACTION_SUCCESS.getCode(),
                ErrorConstant.DELETE_TRANSACTION_SUCCESS.getMessage(),
                toMutationResponse(transaction,
                        wallet != null ? wallet.getBalance() : null,
                        wallet != null ? wallet.getTotalIncome() : null,
                        wallet != null ? wallet.getTotalExpense() : null)
        );
    }

    /** Budgets of the old and the new category / day are checked once this transaction commits. */
    private void publishChange(String memberId, String categoryId, LocalDate localDate,
                               String oldCategoryId, LocalDate oldLocalDate) {
        Set<String> categories = new HashSet<>();
        Set<LocalDate> days = new HashSet<>();
        if (categoryId != null) {
            categories.add(categoryId);
        }
        if (oldCategoryId != null) {
            categories.add(oldCategoryId);
        }
        if (localDate != null) {
            days.add(localDate);
        }
        if (oldLocalDate != null && !Objects.equals(oldLocalDate, localDate)) {
            days.add(oldLocalDate);
        }
        eventPublisher.publishEvent(new TransactionChangedEvent(memberId, categories, days));
    }

    private TransactionMutationResponse toMutationResponse(Transaction transaction, BigDecimal balance,
                                                           BigDecimal totalIncome, BigDecimal totalExpense) {
        TransactionMutationResponse response = new TransactionMutationResponse();
        response.setTransaction(mapToHistoryResponse(transaction,
                categoryService.byIds(Collections.singletonList(transaction.getCategoryId()))));
        response.setBalance(balance);
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        return response;
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

    private TransactionHistoryResponse mapToHistoryResponse(Transaction transaction, Map<String, FinanceCategory> categories) {
        return TransactionMapper.toHistoryResponse(transaction, categories);
    }


    private List<ChartItemResponse> buildChartWithEmptyBucket(String period, List<Object[]> results, LocalDate today) {
        return switch (period) {
            case "1W" -> buildDailyChart(today.minusDays(6), today, results);
            case "1M" -> buildDailyChart(today.minusMonths(1), today, results);
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

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String label = date.toString();
            chart.add(data.getOrDefault(label, new ChartItemResponse(label, BigDecimal.ZERO, BigDecimal.ZERO)));
        }

        int lastNonEmpty = -1;
        for (int i = chart.size() - 1; i >= 0; i--) {
            ChartItemResponse item = chart.get(i);
            if (item.getIncome().compareTo(BigDecimal.ZERO) != 0 ||
                    item.getExpense().compareTo(BigDecimal.ZERO) != 0) {
                lastNonEmpty = i;
                break;
            }
        }

        return lastNonEmpty >= 0 ? chart.subList(0, lastNonEmpty + 1) : chart;
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
