package com.vyra.be_virtual_your_assets.handler;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponse<Void>> handleForbiddenException(ForbiddenException ex) {
        log.warn("ForbiddenException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new BaseResponse<>(
                        ex.getErrorConstant().getCode(),
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<BaseResponse<Void>> handleTooManyRequests(TooManyRequestsException ex) {
        log.warn("TooManyRequestsException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(new BaseResponse<>(
                        ex.getErrorConstant().getCode(),
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<BaseResponse<Void>> handleLockTimeout(Exception ex) {
        // A wallet row (or another row) stayed locked longer than lock_timeout / statement_timeout.
        log.warn("Database lock or statement timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "2")
                .body(new BaseResponse<>(
                        ErrorConstant.SYSTEM_BUSY.getCode(),
                        ErrorConstant.SYSTEM_BUSY.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                new BaseResponse<>(
                        ex.getErrorConstant().getCode(),
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequestException(BadRequestException ex) {
        log.warn("BadRequestException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                new BaseResponse<>(
                        ex.getErrorConstant().getCode(),
                        ex.getErrorConstant().getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<BaseResponse<Void>> handleNpe(NullPointerException ex) {
        log.error("NullPointerException", ex);
        return ResponseEntity.internalServerError().body(
                new BaseResponse<>(
                        ErrorConstant.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorConstant.INTERNAL_SERVER_ERROR.getMessage(),
                        null
                )
        );
    }

    /** Unknown path (or a disabled one such as /swagger-ui.html in production): 404, not 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new BaseResponse<>(
                        ErrorConstant.DATA_NOT_FOUND.getCode(),
                        ErrorConstant.DATA_NOT_FOUND.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(
                new BaseResponse<>(
                        ErrorConstant.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorConstant.INTERNAL_SERVER_ERROR.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();
        return ResponseEntity.badRequest().body(
                new BaseResponse<>(
                        ErrorConstant.BAD_REQUEST.getCode(),
                        message,
                        null
                )
        );
    }


}
