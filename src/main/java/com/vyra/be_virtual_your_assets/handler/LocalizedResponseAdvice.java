package com.vyra.be_virtual_your_assets.handler;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.util.ApiLocale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Objects;

/**
 * Puts every {@link BaseResponse} message (controllers and {@link GlobalExceptionHandler} alike) in
 * the language the client asked for. Services keep building English text from ErrorConstant; it is
 * swapped for the constant's Indonesian text here.
 */
@RestControllerAdvice
public class LocalizedResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof BaseResponse<?> base)) return body;
        String localized = ApiLocale.localize(base.getResponseStatus(), base.getResponseMessage(),
                LocaleContextHolder.getLocale());
        if (Objects.equals(localized, base.getResponseMessage())) return body;
        // A copy: the original may be kept and replayed (e.g. an idempotent request) to another client.
        return new BaseResponse<>(base.getResponseStatus(), localized, base.getData());
    }
}
