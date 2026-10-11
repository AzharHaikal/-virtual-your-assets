package com.vyra.be_virtual_your_assets.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceEncryptConverterTest {

    @Mock private CryptoService cryptoService;

    @InjectMocks
    private BalanceEncryptConverter converter;

    @Test
    void convertToDatabaseColumn_null_shouldReturnNullWithoutEncrypting() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        verifyNoInteractions(cryptoService);
    }

    @Test
    void convertToDatabaseColumn_value_shouldEncryptPlainString() {
        when(cryptoService.encrypt("1500.25")).thenReturn("cipher");

        assertThat(converter.convertToDatabaseColumn(new BigDecimal("1500.25"))).isEqualTo("cipher");
    }

    @Test
    void convertToEntityAttribute_null_shouldReturnNullWithoutDecrypting() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        verifyNoInteractions(cryptoService);
    }

    @Test
    void convertToEntityAttribute_value_shouldDecryptToBigDecimal() {
        when(cryptoService.decrypt("cipher")).thenReturn("1500.25");

        assertThat(converter.convertToEntityAttribute("cipher")).isEqualByComparingTo("1500.25");
    }
}
