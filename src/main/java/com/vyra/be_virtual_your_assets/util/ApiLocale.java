package com.vyra.be_virtual_your_assets.util;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Language of the API's response messages. The app sends {@code Accept-Language: id} or {@code en}
 * (its language setting); Indonesian is used for {@code id}, English for anything else and when the
 * header is missing. The admin portal always asks for English.
 */
public final class ApiLocale {

    public static final Locale INDONESIAN = Locale.forLanguageTag("id");
    public static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, INDONESIAN);

    private static final Map<String, List<ErrorConstant>> BY_CODE = Arrays.stream(ErrorConstant.values())
            .collect(Collectors.groupingBy(ErrorConstant::getCode));

    /** "14 minutes", "1 second": the wait appended to lock-out and cool-down messages. */
    private static final Pattern DURATION = Pattern.compile("(\\d+) (second|minute)s?\\b");

    private ApiLocale() {
    }

    public static Locale of(HttpServletRequest request) {
        return fromHeader(request.getHeader("Accept-Language"));
    }

    public static Locale fromHeader(String acceptLanguage) {
        if (StringUtils.isBlank(acceptLanguage)) return Locale.ENGLISH;
        try {
            Locale match = Locale.lookup(Locale.LanguageRange.parse(acceptLanguage), SUPPORTED);
            return match != null ? match : Locale.ENGLISH;
        } catch (IllegalArgumentException malformed) {
            return Locale.ENGLISH;
        }
    }

    public static boolean isIndonesian(Locale locale) {
        return locale != null && INDONESIAN.getLanguage().equals(locale.getLanguage());
    }

    /**
     * The response message in {@code locale}. Text that is a constant's English message, possibly
     * followed by a detail (remaining attempts, "14 minutes."), becomes that constant's text in the
     * locale. Anything else (an already translated validation message, the maintenance text typed in
     * the portal) is returned as it is.
     */
    public static String localize(String code, String message, Locale locale) {
        if (!isIndonesian(locale) || code == null || message == null) return message;
        List<ErrorConstant> candidates = BY_CODE.get(code);
        if (candidates == null) return message;
        for (ErrorConstant constant : candidates) {
            if (message.equals(constant.getMessage())) return constant.getMessage(locale);
        }
        for (ErrorConstant constant : candidates) {
            String english = constant.getMessage();
            if (!english.isEmpty() && message.startsWith(english)) {
                return constant.getMessage(locale) + localizeDetail(message.substring(english.length()));
            }
        }
        return message;
    }

    /** " 14 minutes." becomes " 14 menit."; numbers and punctuation stay. */
    static String localizeDetail(String detail) {
        Matcher m = DURATION.matcher(detail);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, m.group(1) + ("second".equals(m.group(2)) ? " detik" : " menit"));
        }
        m.appendTail(out);
        return out.toString();
    }
}
