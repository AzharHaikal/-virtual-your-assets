package com.vyra.be_virtual_your_assets.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoServiceTest {

    private static final String KEY = "1234567890123456"; // 16 chars -> AES-128

    private final CryptoService cryptoService = new CryptoService(KEY);

    @Test
    void encryptThenDecrypt_shouldRoundTrip() {
        String encrypted = cryptoService.encrypt("1250000.50");

        assertThat(encrypted).isNotEqualTo("1250000.50");
        assertThat(cryptoService.decrypt(encrypted)).isEqualTo("1250000.50");
    }

    @Test
    void encrypt_samePlaintextTwice_shouldProduceDifferentCiphertext() {
        // random IV per call
        assertThat(cryptoService.encrypt("100")).isNotEqualTo(cryptoService.encrypt("100"));
    }

    @Test
    void decrypt_withDifferentKey_shouldFail() {
        String encrypted = cryptoService.encrypt("100");
        CryptoService other = new CryptoService("6543210987654321");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> other.decrypt(encrypted));
        assertThat(ex.getMessage()).isEqualTo("Decrypt failed");
    }

    @Test
    void decrypt_tamperedPayload_shouldFail() {
        String encrypted = cryptoService.encrypt("100");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThrows(RuntimeException.class, () -> cryptoService.decrypt(tampered));
    }

    @Test
    void decrypt_notBase64_shouldFail() {
        assertThrows(RuntimeException.class, () -> cryptoService.decrypt("%%% not base64 %%%"));
    }

    @Test
    void encrypt_withInvalidKeyLength_shouldFailWithEncryptMessage() {
        CryptoService badKey = new CryptoService("short");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> badKey.encrypt("100"));
        assertThat(ex.getMessage()).isEqualTo("Encrypt failed");
    }

    @Test
    void encrypt_nullValue_shouldFail() {
        assertThrows(RuntimeException.class, () -> cryptoService.encrypt(null));
    }

    @Test
    void converter_realRoundTrip_shouldPreserveScale() {
        BalanceEncryptConverter converter = new BalanceEncryptConverter();
        ReflectionTestUtils.setField(converter, "cryptoService", cryptoService);

        String stored = converter.convertToDatabaseColumn(new BigDecimal("42.10"));

        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(new BigDecimal("42.10"));
    }
}
