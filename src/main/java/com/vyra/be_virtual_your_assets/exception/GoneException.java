package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

/**
 * The resource existed but is no longer available (e.g. an expired export download link).
 * Mapped to HTTP 410 by {@code GlobalExceptionHandler}.
 */
@Getter
public class GoneException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public GoneException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }
}
