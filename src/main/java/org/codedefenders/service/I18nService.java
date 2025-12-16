/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import com.google.gson.JsonParser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static org.codedefenders.database.AdminDAO.getSystemSetting;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.SUPPORTED_LANGUAGES;
import static org.xnap.commons.i18n.I18nFactory.FALLBACK;
import static org.xnap.commons.i18n.I18nFactory.READ_PROPERTIES;


@Named
@ApplicationScoped
public class I18nService {

    private static final Logger logger = LoggerFactory.getLogger(I18nService.class);
    private static final Locale FALLBACK_LOCALE = Locale.US;

    private final Set<String> javascriptStrings = readI18nJsJson();

    // inject
    private final CodeDefendersAuth login;

    @Inject
    public I18nService(CodeDefendersAuth login) {
        this.login = login;
    }


    // JS localize

    public Set<String> getJavascriptStrings() {
        return new HashSet<>(javascriptStrings);
    }

    private Set<String> readI18nJsJson() {
        final Set<String> set = new HashSet<>();
        String json;

        var url = Thread.currentThread().getContextClassLoader().getResource("i18n.js.json");
        if (url == null) {
            logger.error("i18n.js.json not found");
            return set;
        }

        try {
            var path = Path.of(url.toURI());
            json = Files.readString(path, UTF_8);
            logger.info("i18n.js.json loaded: {}", json);
        } catch (URISyntaxException | IOException ex) {
            logger.error(ex.getMessage());
            return set;
        }

        JsonParser
                .parseString(json)
                .getAsJsonObject()
                .getAsJsonArray("strings")
                .forEach(string -> set.add(string.getAsString()));

        return set;
    }

    /**
     * JS strings need to be unescaped before translation, then escaped again when printed into a JS script.
     */
    public String unescape(String string) {
        return StringEscapeUtils.unescapeJson(string);
    }

    /**
     * Escapes sequences, so that it can be printed as JS string.
     */
    public String escape(String string) {
        return StringEscapeUtils.escapeJson(string);
    }


    // determine/setting user locale

    /**
     * @return Array of locales set in the admin settings. Always contains at least one fallback locale.
     */
    public Locale[] getSupportedLocales() {
        var supportedLanguageSetting = getSystemSetting(SUPPORTED_LANGUAGES).getStringValue();
        var locales = Arrays.stream(supportedLanguageSetting.split("[,;]"))
                .filter(not(String::isBlank)).map(Locale::new).toArray(Locale[]::new);
        if (locales.length == 0) {
            logger.warn("no locales set in the admin settings, falling back to English");
            return new Locale[] {FALLBACK_LOCALE};
        } else {
            return locales;
        }
    }

    public Locale getDefaultLocale() {
        return getSupportedLocales()[0];
    }

    public Locale toSupportedLocale(String language) {
        var supportedLocales = getSupportedLocales();
        var locale = new Locale(language);
        if (Arrays.asList(supportedLocales).contains(locale)) {
            return locale;
        } else {
            return supportedLocales[0];
        }
    }


    // supply i18n instance

    public static I18n getI18n(Locale pLocale) {
        return I18nFactory.getI18n(
                I18nService.class,
                pLocale == null ? Locale.getDefault() : pLocale,
                FALLBACK | READ_PROPERTIES
        );
    }

    /**
     * Get the i18n instance for one of the supported locales.
     * Considers user preference and browser locale, falls back to the default locale.
     *
     * @param request The request is used to determine the browser locale transmitted via the Accept-Language header.
     * @return The i18n instance.
     */
    public I18n getI18n(ServletRequest request) {
        var user = login.isLoggedIn() ? login.getUser() : null;
        var lang = Optional.ofNullable(user)
                .map(User::getLocale)
                .orElseGet(() -> getSessionLocale(request))
                .getLanguage();

        return getI18n(toSupportedLocale(lang));
    }

    /**
     * Tries to get the session locale if set, or uses the request locale as fallback else.
     *
     * @param request The current HttpServletRequest
     * @return The locale
     */
    public Locale getSessionLocale(ServletRequest request) {
        var httpReq = (HttpServletRequest) request;
        var session = httpReq.getSession();
        var locale = session.getAttribute("locale");
        return locale != null ? (Locale) locale : request.getLocale();
    }
}
