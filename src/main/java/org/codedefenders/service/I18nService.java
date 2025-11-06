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
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

import static java.nio.charset.StandardCharsets.UTF_8;


@Named
@ApplicationScoped
public class I18nService {

    private static final Logger logger = LoggerFactory.getLogger(I18nService.class);

    private final Set<String> javascriptStrings = readI18nJsJson();

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
}
