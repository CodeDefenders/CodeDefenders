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
 * Reads configuration values from system properties after converting the attribute name to lower.dot.separated format.
 *
 * @author degenhart
 */
@Singleton
public class SystemPropertySource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(SystemPropertySource.class);

    public String resolveAttributeName(String camelCaseName) {
        return "codedefenders." + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCaseName).replace('-', '.');
    }

    @Override
    public Optional<String> resolveAttribute(String camelCaseName) {
        return Optional.ofNullable(System.getProperty(resolveAttributeName(camelCaseName)));
    }

    @Override
    public int getPriority() {
        return 70;
    }
}
