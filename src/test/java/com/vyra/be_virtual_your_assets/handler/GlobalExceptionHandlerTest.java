package com.vyra.be_virtual_your_assets.handler;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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
