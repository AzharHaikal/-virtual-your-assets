package com.vyra.be_virtual_your_assets.handler;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // =========================================================================
    // handleForbiddenException
    // =========================================================================
    @Test
    void handleForbiddenException_shouldReturn403WithErrorPayload() {
        ForbiddenException ex = new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED);

        ResponseEntity<BaseResponse<Void>> response = handler.handleForbiddenException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED.getCode());
        assertThat(response.getBody().getResponseMessage()).isEqualTo(ErrorConstant.ADMIN_UNAUTHORIZED.getMessage());
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void handleNotFound_shouldReturn404WithTheResourceCode() {
        ResponseEntity<BaseResponse<Void>> response =
                handler.handleNotFound(new NotFoundException(ErrorConstant.TRANSACTION_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.TRANSACTION_NOT_FOUND.getCode());
    }

    // =========================================================================
    // handleNoResource
    // =========================================================================
    @Test
    void handleNoResource_shouldReturn404InsteadOf500() {
        NoResourceFoundException ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "swagger-ui.html");

        ResponseEntity<BaseResponse<Void>> response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.DATA_NOT_FOUND.getCode());
    }

    // =========================================================================
    // handleTooManyRequests
    // =========================================================================
    @Test
    void handleTooManyRequests_shouldReturn429WithRetryAfterAndRemainingWait() {
        TooManyRequestsException ex = new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 14 * 60 + 1);

        ResponseEntity<BaseResponse<Void>> response = handler.handleTooManyRequests(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("841");
        assertThat(response.getBody().getResponseStatus()).isEqualTo("VYRA-LCK-001");
        assertThat(response.getBody().getResponseMessage()).endsWith("Please try again in 15 minutes.");
    }

    @Test
    void tooManyRequests_shortWaitIsShownInSeconds() {
        assertThat(TooManyRequestsException.humanize(1)).isEqualTo("1 second");
        assertThat(TooManyRequestsException.humanize(45)).isEqualTo("45 seconds");
        assertThat(TooManyRequestsException.humanize(60)).isEqualTo("1 minute");
        assertThat(TooManyRequestsException.humanize(61)).isEqualTo("2 minutes");
    }

    // =========================================================================
    // handleBusinessException
    // =========================================================================
    @Test
    void handleBusinessException_shouldReturn400WithErrorPayload() {
        BusinessException ex = new BusinessException(ErrorConstant.MEMBER_NOT_FOUND);

        ResponseEntity<BaseResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND.getCode());
    }

    // =========================================================================
    // handleBadRequestException
    // =========================================================================
    @Test
    void handleBadRequestException_shouldReturn400WithErrorConstantMessage() {
        BadRequestException ex = new BadRequestException(ErrorConstant.BAD_REQUEST);

        ResponseEntity<BaseResponse<Void>> response = handler.handleBadRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.BAD_REQUEST.getCode());
        assertThat(response.getBody().getResponseMessage()).isEqualTo(ErrorConstant.BAD_REQUEST.getMessage());
    }

    // =========================================================================
    // handleNpe
    // =========================================================================
    @Test
    void handleNpe_shouldReturn500WithGenericErrorPayload() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleNpe(new NullPointerException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.INTERNAL_SERVER_ERROR.getCode());
    }

    // =========================================================================
    // handleGenericException
    // =========================================================================
    @Test
    void handleGenericException_shouldReturn500WithGenericErrorPayload() {
        ResponseEntity<BaseResponse<Void>> response = handler.handleGenericException(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.INTERNAL_SERVER_ERROR.getCode());
    }

    // =========================================================================
    // handleValidationException
    // =========================================================================
    @Test
    void handleValidationException_shouldReturn400WithFirstFieldErrorMessage() {
        FieldError fieldError = new FieldError("request", "email", "Email is required");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<BaseResponse<Void>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getResponseStatus()).isEqualTo(ErrorConstant.BAD_REQUEST.getCode());
        assertThat(response.getBody().getResponseMessage()).isEqualTo("Email is required");
    }
}
