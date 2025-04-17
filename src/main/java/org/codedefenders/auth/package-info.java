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
/**
 * This package contains most Authentication and Authorization related stuff.
 *
 * <p>We utilize Apache Shiro for Authentication and Authorization.<br>
 * In the {@code web.xml} is the {@link org.apache.shiro.web.servlet.ShiroFilter} included.<br>
 * This filter needs/loads a {@link org.apache.shiro.web.env.WebEnvironment} that is configured via
 * the {@code shiroEnvironmentClass} context parameter in the {@code web.xml}.<br>
 * In our case that is the {@link org.codedefenders.auth.CodeDefendersWebEnvironment} because it is directly
 * instantiated by Shiro, CDI does not work in this class (except via {@link org.codedefenders.util.CDIUtil}).<br>
 *
 * <p>The {@code WebEnvironment} depends on:
 * <ul>
 *     <li>a {@link org.apache.shiro.web.mgt.WebSecurityManager} which in turn needs a
 *         {@link org.apache.shiro.realm.Realm}.</li>
 *     <li>a {@link org.apache.shiro.web.filter.mgt.FilterChainResolver}.</li>
 * </ul>
 *
 * <p>Both are configured programmatically in {@link org.codedefenders.auth.ShiroConfig}.
 *
 * <p>For the {@code Realm} required by the {@code WebSecurityManager} we use the
 * {@link org.codedefenders.auth.CodeDefendersRealm} it handles everything related to Authentication and Authorization.
 *
 * <p>For the {@code FilterChainResolver} we have a custom/adapted
 * {@link org.apache.shiro.web.filter.authc.FormAuthenticationFilter}, the
 * {@link org.codedefenders.servlets.auth.CodeDefendersFormAuthenticationFilter}.<br>
 * It mainly implements/adds logging for failed login attempts, user session recording, and filtering of saved urls,
 * so users are not redirected to an {@code /api/} url after successful login.
 */
package org.codedefenders.auth;
