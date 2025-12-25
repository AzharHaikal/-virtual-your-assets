//package com.vyra.virtual_your_assets.exception;
//
//import com.vyra.virtual_your_assets.constant.ErrorConstant;
//import org.springframework.http.HttpStatus;
//
//public class BaseException extends RuntimeException {
//
//    private final String code;
//    private final String message;
//    private final HttpStatus httpStatus;
//
//    public BaseException(String code, String message, HttpStatus httpStatus, Throwable t) {
//        super(t);
//        this.code = code;
//        this.message = message;
//        this.httpStatus = httpStatus;
//    }
//
//    public String getCode() {
//        return code;
//    }
//
//    @Override
//    public String getMessage() {
//        return message;
//    }
//
//    public HttpStatus getHttpStatus() {
//        return httpStatus;
//    }
//}
//
