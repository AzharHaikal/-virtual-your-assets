package com.vyra.be_virtual_your_assets.handler;

import com.vyra.be_virtual_your_assets.config.LocaleConfig;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.RegisterRequest;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Response messages follow Accept-Language: Indonesian for id, English otherwise. */
class LocalizedResponseTest {

    @RestController
    static class SampleController {
        @GetMapping("/ok")
        BaseResponse<Void> ok() {
            return new BaseResponse<>(ErrorConstant.LOGIN_SUCCESS.getCode(), ErrorConstant.LOGIN_SUCCESS.getMessage(), null);
        }

        @GetMapping("/wrong-otp")
        BaseResponse<Void> wrongOtp() {
            throw new BusinessException(ErrorConstant.OTP_INVALID, "2");
        }

        @GetMapping("/locked")
        BaseResponse<Void> locked() {
            throw new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 14 * 60);
        }

        @PostMapping("/register")
        BaseResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
            return new BaseResponse<>(ErrorConstant.REGISTER_SUCCESS.getCode(), ErrorConstant.REGISTER_SUCCESS.getMessage(), null);
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler(LocalizationTestSupport.messageSource()),
                        new LocalizedResponseAdvice())
                .setLocaleResolver(new LocaleConfig().localeResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void successMessage_isIndonesianForId_andEnglishOtherwise() throws Exception {
        mvc.perform(get("/ok").header("Accept-Language", "id"))
                .andExpect(jsonPath("$.responseMessage").value("Anda berhasil masuk"));
        mvc.perform(get("/ok").header("Accept-Language", "id-ID,id;q=0.9,en;q=0.8"))
                .andExpect(jsonPath("$.responseMessage").value("Anda berhasil masuk"));
        mvc.perform(get("/ok").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.responseMessage").value("You've successfully logged in"));
        mvc.perform(get("/ok"))
                .andExpect(jsonPath("$.responseMessage").value("You've successfully logged in"));
        mvc.perform(get("/ok").header("Accept-Language", "fr"))
                .andExpect(jsonPath("$.responseMessage").value("You've successfully logged in"));
    }

    @Test
    void errorWithDetail_keepsTheDetail() throws Exception {
        mvc.perform(get("/wrong-otp").header("Accept-Language", "id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseStatus").value(ErrorConstant.OTP_INVALID.getCode()))
                .andExpect(jsonPath("$.responseMessage")
                        .value("OTP tidak valid. Silakan masukkan OTP dengan benar. Sisa percobaan - 2"));
    }

    @Test
    void lockout_translatesTheWait() throws Exception {
        mvc.perform(get("/locked").header("Accept-Language", "id"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.responseMessage").value(
                        "Terlalu banyak percobaan gagal. Akun Anda dikunci sementara. Silakan coba lagi dalam 14 menit."));
        mvc.perform(get("/locked"))
                .andExpect(jsonPath("$.responseMessage").value(
                        "Too many failed attempts. Your account is temporarily locked. Please try again in 14 minutes."));
    }

    @Test
    void validationMessage_comesFromMessagesId() throws Exception {
        String body = """
                {"firstName":"Muhammad Abdurrahman Wahid","lastName":"Haikal","email":"azhar@gmail.com",
                 "phoneNumber":"6281234567890","pin":"123456"}""";
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Accept-Language", "id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseMessage").value("Nama depan maksimal 20 karakter"));
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.responseMessage").value("First name must not exceed 20 characters"));
    }

    @Test
    void unreadableBody_isTranslated() throws Exception {
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content("{nope")
                        .header("Accept-Language", "id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseMessage").value("Isi permintaan tidak valid"));
    }
}
