package com.vyra.be_virtual_your_assets.dto.export;

/** A file to download. */
public record ExportFile(String fileName, String contentType, byte[] content) {
}
