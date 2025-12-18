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
package org.codedefenders.util.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.codedefenders.configuration.source.ConfigurationSource;
import org.codedefenders.util.WeldExtension;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * A config source builder for setting configuration values in tests.
 *
 * <p>Example usage (with {@link WeldExtension}):
 * <pre>
 *   {@code @Produces} ConfigurationSource config = new ConfigurationSourceBuilder()
 *       .withConfigFile(Path.of("/srv/codedefenders.properties"))
 *       .withProperty("dbConnectionsMax", "123");
 * </pre>
 * <p>See {@link BaseConfiguration} for possible config files keys.
 */
public class ConfigurationSourceBuilder implements ConfigurationSource {
    private Map<String, String> values = new HashMap<>();

    @Override
    public Optional<String> resolveAttribute(String camelCaseName) {
        return Optional.ofNullable(values.get(camelCaseName));
    }

    @Override
    public int getPriority() {
        return 9999;
    }

    public ConfigurationSourceBuilder withProperty(String camelCaseName, String value) {
        values.put(camelCaseName, value);
        return this;
    }

    public ConfigurationSourceBuilder withConfigFile(Path configFilePath) {
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(configFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            var words = ((String) entry.getKey()).split("\\.");
            for (int i = 1; i < words.length; i++) {
                words[i] = StringUtils.capitalize(words[i]);
            }
            String camelCaseName = Arrays.stream(words).collect(Collectors.joining());
            values.put(camelCaseName, String.valueOf(entry.getValue()));
        }
        return this;
    }
}
