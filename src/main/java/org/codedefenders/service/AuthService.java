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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.shiro.SecurityUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.auth.permissions.AdminPermission;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("auth")
@ApplicationScoped
public class AuthService implements CodeDefendersAuth {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final CodeDefendersRealm codeDefendersRealm;

    private final UserService userService;

    @Inject
    public AuthService(CodeDefendersRealm codeDefendersRealm, UserService userService) {
        this.codeDefendersRealm = codeDefendersRealm;
        this.userService = userService;
    }

    @Override
    public boolean isLoggedIn() {
        return SecurityUtils.getSubject().isAuthenticated();
    }

    @Override
    public boolean isAdmin() {
        return SecurityUtils.getSubject().isPermitted(AdminPermission.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUserId() {
        try {
            return SecurityUtils.getSubject().getPrincipals().oneByType(CodeDefendersRealm.LocalUserId.class).getUserId();
        } catch (NullPointerException e) {
            //This happens when the user is not logged in and should usually not be a reason for concern.
            logger.debug(e.toString());
            return -1;
        }
    }

    @Override
    public SimpleUser getSimpleUser() {
        return userService.getSimpleUserById(getUserId()).orElse(null);
    }

    @Override
    public User getUser() {
        return userService.getUserById(getUserId()).orElse(null);
    }

    protected void invalidate(int userId) {
        codeDefendersRealm.invalidate(userId);
    }
}
