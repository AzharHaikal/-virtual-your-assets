package com.vyra.virtual_your_assets.handler;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.exception.BadRequestException;
import com.vyra.virtual_your_assets.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
