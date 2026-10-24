package com.vyra.be_virtual_your_assets.constant;

/** A personal data export: waiting, being built, downloadable (24 hours), failed, or emptied after expiring. */
public enum DataExportStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED,
    EXPIRED
}
