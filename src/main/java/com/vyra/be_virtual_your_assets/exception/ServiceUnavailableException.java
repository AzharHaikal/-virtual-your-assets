package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

/**
 * An optional external service (e.g. the AI model) is switched off or failing.
 * Mapped to HTTP 503 by {@code GlobalExceptionHandler}; the code tells it apart from maintenance.
 */
@Getter
public class ServiceUnavailableException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public ServiceUnavailableException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }
}
