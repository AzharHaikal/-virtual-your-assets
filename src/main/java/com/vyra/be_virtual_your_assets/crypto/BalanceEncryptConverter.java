package com.vyra.be_virtual_your_assets.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Converter
public class BalanceEncryptConverter implements AttributeConverter<BigDecimal, String> {

    @Autowired
    private CryptoService cryptoService;

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) return null;
        return cryptoService.encrypt(attribute.toString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return new BigDecimal(cryptoService.decrypt(dbData));
    }
}