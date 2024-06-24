/*
 * Copyright (C) 2020 Code Defenders contributors
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

package org.codedefenders.configuration.implementation;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

/**
 * Reads configuration values from the context after converting the attribute name to lower.dot.separated format.
 *
 * @author degenhart
 */
@Priority(10)
@Alternative
@Singleton
class ContextConfiguration extends BaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ContextConfiguration.class);

    @Override
    protected String resolveAttributeName(String camelCaseName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCaseName).replace('-', '.');
    }

    @Override
    protected Object resolveAttribute(String camelCaseName) {
        try {
            Context initialContext = new InitialContext();
            return initialContext.lookup("java:comp/env/codedefenders/" + resolveAttributeName(camelCaseName));
        } catch (NamingException e) {
            return null;
        }
    }
}

