package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

/**
 * Thrown when an authenticated admin does not have sufficient permission
 * (role-based access control) to perform the requested action.
 * Mapped to HTTP 403 by {@code GlobalExceptionHandler}.
 */
@Getter
public class ForbiddenException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public ForbiddenException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }
}
