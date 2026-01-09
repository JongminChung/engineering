package io.github.jongminchung.study.apicommunication.context;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** Resolves the response language according to the Accept-Language header. */
@Component
public class ResponseLanguageResolver {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.KOREAN, Locale.ENGLISH);
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    public String resolve(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT_LOCALE.toLanguageTag();
        }
        try {
            List<Locale.LanguageRange> languageRanges = Locale.LanguageRange.parse(acceptLanguage);
            Locale match = Locale.lookup(languageRanges, SUPPORTED_LOCALES);
            return match == null ? DEFAULT_LOCALE.toLanguageTag() : match.toLanguageTag();
        } catch (IllegalArgumentException _) {
            return DEFAULT_LOCALE.toLanguageTag();
        }
    }
}
