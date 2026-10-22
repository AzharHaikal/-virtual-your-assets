package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

/** The request conflicts with one already received (e.g. an Idempotency-Key in use). HTTP 409. */
@Getter
public class ConflictException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public ConflictException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }
}
