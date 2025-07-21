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
package org.codedefenders.configuration.configfileresolver;

import java.io.Reader;

import jakarta.inject.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class ContextConfigFileResolver extends ConfigFileResolver {
    private static final Logger logger = LoggerFactory.getLogger(ContextConfigFileResolver.class);

    @Override
    public Reader getConfigFile(String filename) {
        String filePath;
        try {
            Context initialContext = new InitialContext();
            filePath = (String) initialContext.lookup("java:comp/env/codedefenders/config");
        } catch (NamingException e) {
            return null;
        }
        return getConfigFileImpl(filePath, filename);
    }
}
