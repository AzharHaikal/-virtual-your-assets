package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

/**
 * The requested resource does not exist <b>or does not belong to the caller</b> (both look the same,
 * so ids of other members cannot be probed). Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
@Getter
public class NotFoundException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public NotFoundException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }
}
