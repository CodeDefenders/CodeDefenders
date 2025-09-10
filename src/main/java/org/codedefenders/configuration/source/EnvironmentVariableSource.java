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
package org.codedefenders.configuration.source;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

/**
 * Reads configuration values from environment variables after converting the attribute name to UPPER_UNDERSCORE format
 * and prefixing them with {@code CODEDEFENDERS_}.
 *
 * @author degenhart
 */
@Singleton
public class EnvironmentVariableSource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentVariableSource.class);

    public String resolveAttributeName(String camelCaseName) {
        return "CODEDEFENDERS_" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelCaseName);
    }

    @Override
    public Optional<String> resolveAttribute(String camelCaseName) {
        return Optional.ofNullable(
            System.getenv(resolveAttributeName(camelCaseName)));
    }

    @Override
    public int getPriority() {
        return 50;
    }
}

