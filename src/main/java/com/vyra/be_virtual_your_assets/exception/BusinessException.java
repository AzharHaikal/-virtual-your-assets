package com.vyra.be_virtual_your_assets.exception;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public BusinessException(ErrorConstant errorConstant) {
        super(errorConstant.getMessage());
        this.errorConstant = errorConstant;
    }

    public BusinessException(ErrorConstant errorConstant, String additionalMessage) {
        super(errorConstant.getMessage() + additionalMessage);
        this.errorConstant = errorConstant;
    }
}
