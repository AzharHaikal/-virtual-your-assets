package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Texts of the notifications the server creates, in the member's language (stored with the notification,
 * so a later language change keeps the old ones as they were).
 */
public final class NotificationMessages {

    public record Message(String title, String body) {
    }

    public static final int MAX_TITLE = 120;
    public static final int MAX_BODY = 500;

    private NotificationMessages() {
    }

    public static Message habitReminder(AppLanguage language, String habitName) {
        return id(language)
                ? message("Waktunya " + habitName, "Jaga konsistensimu, tandai selesai hari ini.")
                : message("Time for " + habitName, "Keep your streak going: mark it done today.");
    }

    public static Message reportResolved(AppLanguage language) {
        return id(language)
                ? message("Laporan Anda sudah ditangani",
                        "Terima kasih sudah melapor. Tim VYRA telah menindaklanjuti laporan Anda.")
                : message("Your report was handled",
                        "Thank you for reporting. The VYRA team has followed up on your report.");
    }

    /** {@code percent} 80..100+: from 100 the budget is used up. */
    public static Message budgetAlert(AppLanguage language, String categoryName, int percent) {
        if (percent >= 100) {
            return id(language)
                    ? message("Anggaran " + categoryName + " habis",
                            "Pengeluaran " + categoryName + " bulan ini sudah " + percent + "% dari anggaran.")
                    : message(categoryName + " budget used up",
                            "You have spent " + percent + "% of your " + categoryName + " budget this month.");
        }
        return id(language)
                ? message("Anggaran " + categoryName + " sudah " + percent + "%",
                        "Pengeluaran " + categoryName + " bulan ini mendekati batas anggaran.")
                : message(categoryName + " budget at " + percent + "%",
                        "Your " + categoryName + " spending this month is close to the budget.");
    }

    public static Message goalDue(AppLanguage language, String goalTitle, LocalDate dueDate) {
        return id(language)
                ? message(goalTitle + " jatuh tempo 3 hari lagi",
                        "Tenggat " + dueDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id")))
                                + ". Yuk cek progresmu.")
                : message(goalTitle + " is due in 3 days",
                        "Due " + dueDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
                                + ". Take a look at your progress.");
    }

    private static boolean id(AppLanguage language) {
        return language == AppLanguage.ID;
    }

    private static Message message(String title, String body) {
        return new Message(StringUtils.abbreviate(title, MAX_TITLE), StringUtils.abbreviate(body, MAX_BODY));
    }
}
