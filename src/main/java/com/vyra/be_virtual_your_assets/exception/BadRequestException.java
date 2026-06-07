package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public BadRequestException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }
}
