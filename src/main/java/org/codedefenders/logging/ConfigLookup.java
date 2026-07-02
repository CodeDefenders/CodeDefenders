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
package org.codedefenders.logging;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.codedefenders.configuration.configfileresolver.ClasspathConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.ConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.ContextConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.EnvironmentVariableConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.SystemPropertyConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.TomcatConfigFileResolver;
import org.codedefenders.configuration.source.ConfigurationSource;
import org.codedefenders.configuration.source.ContextSource;
import org.codedefenders.configuration.source.EnvironmentVariableSource;
import org.codedefenders.configuration.source.PropertiesFileSource;
import org.codedefenders.configuration.source.SystemPropertySource;
import org.codedefenders.configuration.source.TieredSource;

/**
 * Custom log4j2 config lookup that provides raw Code Defenders config values via the {@code ${cfg:camelCaseName}} lookup.
 * Has to be specified in the log4j2 config via {@code <Configuration packages="org.codedefenders.logging.ConfigLookup">}.
 * <p>
 * This uses a hard-coded list of configuration sources instead of relying on CDI discovery since the logging config is
 * loaded before the CDI is initialized.
 */
@Plugin(name = "cfg", category = StrLookup.CATEGORY)
public class ConfigLookup extends AbstractLookup {
    private TieredSource source;

    @Override
    public String lookup(LogEvent event, String camelCaseName) {
        if (source == null) {
            source = getDefaultConfigSource();
        }
        return source.resolveAttribute(camelCaseName)
            .orElseThrow(() -> new ConfigLookupException("Config key not found: " + camelCaseName));
    }

    private TieredSource getDefaultConfigSource() {
        return new TieredSource(Stream.of(
            new ContextSource(),
            new EnvironmentVariableSource(),
            new PropertiesFileSource(getDefaultConfigFileResolvers()),
            new SystemPropertySource()
        ).sorted(Comparator.comparing(ConfigurationSource::getPriority))
        .toList());
    }
    private List<ConfigFileResolver> getDefaultConfigFileResolvers() {
        return Stream.of(
            new ClasspathConfigFileResolver(),
            new ContextConfigFileResolver(),
            new EnvironmentVariableConfigFileResolver(),
            new SystemPropertyConfigFileResolver(),
            new TomcatConfigFileResolver()
        ).sorted(Comparator.comparing(ConfigFileResolver::getPriority))
        .toList();
    }

    public static class ConfigLookupException extends RuntimeException {
        public ConfigLookupException(String msg) { super(msg); };
    }
}

