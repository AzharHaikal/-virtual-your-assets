package com.vyra.be_virtual_your_assets.config;

import com.vyra.be_virtual_your_assets.util.ApiLocale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Locale of each request, from its Accept-Language header (see {@link ApiLocale}): English unless
 * the client asks for Indonesian. Never the server's own locale, so a machine set to Indonesian does
 * not change the language for clients that send no header.
 */
@Configuration
public class LocaleConfig {

    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver() {
        return new LocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                return ApiLocale.of(request);
            }

            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
                throw new UnsupportedOperationException("The locale comes from the Accept-Language header");
            }
        };
    }
}
