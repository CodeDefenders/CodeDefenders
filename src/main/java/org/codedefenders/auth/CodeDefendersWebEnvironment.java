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
package org.codedefenders.auth;

import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.codedefenders.util.CDIUtil;

/**
 * Initializes the WebEnvironment for Shiro.
 *
 * @implNote This class is directly instantiated by Shiro.
 *         Shiro bypasses the CDI, so CDI managed beans can only be accessed through {@link CDIUtil#getBeanFromCDI(Class)}
 */
public class CodeDefendersWebEnvironment extends DefaultWebEnvironment {

    public CodeDefendersWebEnvironment() {
        super();
        setSecurityManager(CDIUtil.getBeanFromCDI(WebSecurityManager.class));
        setFilterChainResolver(CDIUtil.getBeanFromCDI(FilterChainResolver.class));
    }
}
