package com.vyra.be_virtual_your_assets.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Stores private free text (e.g. the mood journal) encrypted with AES-256-GCM. */
@Component
@Converter
public class TextEncryptConverter implements AttributeConverter<String, String> {

    @Autowired
    private CryptoService cryptoService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return cryptoService.decrypt(dbData);
    }
}
