package com.vyra.be_virtual_your_assets.event;

import java.time.LocalDate;
import java.util.Set;

/**
 * A member's transaction was created, edited or deleted: the categories and local dates it was (and
 * now is) booked on. Published inside the transaction; listeners run after the commit.
 */
public record TransactionChangedEvent(String memberId, Set<String> categoryIds, Set<LocalDate> localDates) {
}
