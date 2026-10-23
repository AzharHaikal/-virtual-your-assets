package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.be_virtual_your_assets.entity.IdempotencyKey;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String MEMBER = "member-1";
    private static final String KEY = "a1b2c3";
    private static final Map<String, Object> REQUEST = Map.of("amount", 50000, "type", "EXPENSE");

    @Mock private IdempotencyKeyRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private IdempotencyService service;
    private final AtomicInteger runs = new AtomicInteger();

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        service = new IdempotencyService(repository, objectMapper, timeService);
    }

    @Test
    void withoutKey_runsTheActionUnprotected() {
        BaseResponse<CreateTransactionResponse> result = service.execute(MEMBER, null, REQUEST,
                CreateTransactionResponse.class, this::create);

        assertThat(result.getData().getTransactionId()).isEqualTo("trx-1");
        verifyNoInteractions(repository);
    }

    @Test
    void firstRequest_reservesRunsAndStoresTheResponse() {
        when(repository.reserve(eq(MEMBER + ":" + KEY), eq(MEMBER), anyString(), any())).thenReturn(1);

        BaseResponse<CreateTransactionResponse> result = service.execute(MEMBER, KEY, REQUEST,
                CreateTransactionResponse.class, this::create);

        assertThat(runs).hasValue(1);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(repository).complete(eq(MEMBER + ":" + KEY), body.capture());
        assertThat(body.getValue()).contains("trx-1");
        assertThat(result.getResponseStatus()).isEqualTo("VYRA-CTS-000");
        verify(repository).deleteExpired(MEMBER, LocalDateTime.of(2026, 10, 21, 10, 0));
    }

    @Test
    void retryWithSameKeyAndBody_returnsTheStoredResponseWithoutRunning() throws Exception {
        String hash = captureHashOfFirstRequest();
        IdempotencyKey stored = stored(hash, objectMapper.writeValueAsString(
                new BaseResponse<>("VYRA-CTS-000", "ok", response("trx-1"))));
        when(repository.reserve(anyString(), anyString(), anyString(), any())).thenReturn(0);
        when(repository.findById(MEMBER + ":" + KEY)).thenReturn(Optional.of(stored));
        runs.set(0);

        BaseResponse<CreateTransactionResponse> result = service.execute(MEMBER, KEY, REQUEST,
                CreateTransactionResponse.class, this::create);

        assertThat(runs).hasValue(0);
        assertThat(result.getData().getTransactionId()).isEqualTo("trx-1");
        assertThat(result.getData().getBalance()).isEqualByComparingTo("750000");
    }

    @Test
    void sameKeyDifferentBody_isRejected() {
        when(repository.reserve(anyString(), anyString(), anyString(), any())).thenReturn(0);
        when(repository.findById(MEMBER + ":" + KEY)).thenReturn(Optional.of(stored("another-hash", "{}")));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.execute(MEMBER, KEY, REQUEST,
                CreateTransactionResponse.class, this::create));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.IDEMPOTENCY_KEY_REUSED);
        assertThat(runs).hasValue(0);
    }

    @Test
    void firstRequestStillRunning_isRejected() {
        String hash = captureHashOfFirstRequest();
        when(repository.reserve(anyString(), anyString(), anyString(), any())).thenReturn(0);
        when(repository.findById(MEMBER + ":" + KEY)).thenReturn(Optional.of(stored(hash, null)));
        runs.set(0);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.execute(MEMBER, KEY, REQUEST,
                CreateTransactionResponse.class, this::create));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.IDEMPOTENCY_KEY_IN_PROGRESS);
        assertThat(runs).hasValue(0);
    }

    @Test
    void failingAction_freesTheKeyForARetry() {
        when(repository.reserve(anyString(), anyString(), anyString(), any())).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.execute(MEMBER, KEY, REQUEST,
                CreateTransactionResponse.class, () -> { throw new IllegalStateException("db down"); }));

        verify(repository).deleteById(MEMBER + ":" + KEY);
        verify(repository, never()).complete(anyString(), anyString());
    }

    @Test
    void tooLongKey_isRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.execute(MEMBER, "k".repeat(101),
                REQUEST, CreateTransactionResponse.class, this::create));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.IDEMPOTENCY_KEY_INVALID);
    }

    // ------------------------------------------------------------------
    private BaseResponse<CreateTransactionResponse> create() {
        runs.incrementAndGet();
        return new BaseResponse<>("VYRA-CTS-000", "ok", response("trx-" + runs.get()));
    }

    private static CreateTransactionResponse response(String id) {
        CreateTransactionResponse response = new CreateTransactionResponse();
        response.setTransactionId(id);
        response.setBalance(new BigDecimal("750000"));
        return response;
    }

    /** The hash the service computes for REQUEST (taken from a first, successful run). */
    private String captureHashOfFirstRequest() {
        when(repository.reserve(anyString(), anyString(), anyString(), any())).thenReturn(1);
        service.execute(MEMBER, KEY, REQUEST, CreateTransactionResponse.class, this::create);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).reserve(anyString(), anyString(), hash.capture(), any());
        return hash.getValue();
    }

    private static IdempotencyKey stored(String hash, String body) {
        IdempotencyKey key = new IdempotencyKey();
        key.setId(MEMBER + ":" + KEY);
        key.setMemberId(MEMBER);
        key.setRequestHash(hash);
        key.setResponseBody(body);
        return key;
    }
}
