package com.vyra.be_virtual_your_assets.dto;

import com.vyra.be_virtual_your_assets.dto.auth.ResetPinRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPinRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private ResetPinRequest request(String email, String phone, String pin) {
        ResetPinRequest r = new ResetPinRequest();
        r.setEmail(email);
        r.setPhoneNumber(phone);
        r.setNewPin(pin);
        return r;
    }

    @Test
    void validEmailAndSixDigitPin_shouldPass() {
        assertThat(validator.validate(request("budi@example.com", null, "654321"))).isEmpty();
    }

    @Test
    void validPhoneAndSixDigitPin_shouldPass() {
        assertThat(validator.validate(request(null, "628123456789", "654321"))).isEmpty();
    }

    @Test
    void missingIdentifier_shouldFail() {
        assertThat(validator.validate(request(null, null, "654321"))).isNotEmpty();
        assertThat(validator.validate(request("  ", "", "654321"))).isNotEmpty();
    }

    @Test
    void weakOrMalformedPin_shouldFail() {
        assertThat(validator.validate(request("budi@example.com", null, null))).isNotEmpty();
        assertThat(validator.validate(request("budi@example.com", null, "1"))).isNotEmpty();
        assertThat(validator.validate(request("budi@example.com", null, "12345a"))).isNotEmpty();
        assertThat(validator.validate(request("budi@example.com", null, "1234567"))).isNotEmpty();
    }
}
