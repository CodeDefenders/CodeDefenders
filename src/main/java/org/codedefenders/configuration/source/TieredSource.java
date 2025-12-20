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

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merges other configuration sources in the order they are specified and then resolves attributes on the result.
 *
 * @author degenhart
 */
public class TieredSource implements ConfigurationSource {
    private static final Logger logger = LoggerFactory.getLogger(TieredSource.class);

    private final List<ConfigurationSource> sources;

    public TieredSource(List<ConfigurationSource> sources) {
        super();
        this.sources = sources;
    }

    @Override
    public Optional<String> resolveAttribute(String camelCaseName) {
        String propSetBy = null;
        String value = null;
        for (var source : sources) {
            Optional<String> sourceValue = source.resolveAttribute(camelCaseName);
            if (sourceValue.isPresent()) {
                value = sourceValue.get();
                propSetBy = source.getClass().getSimpleName();
                logger.debug("Property " + camelCaseName + " present in " + propSetBy);
            }
        }
        logger.info("Property " + camelCaseName + (propSetBy != null ? " set by " + propSetBy : " not set."));
        return Optional.ofNullable(value);
    }
}
