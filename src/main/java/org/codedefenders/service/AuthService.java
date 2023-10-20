/*
 * Copyright (C) 2022 Code Defenders contributors
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.shiro.SecurityUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;

@Named("auth")
@ApplicationScoped
public class AuthService implements CodeDefendersAuth {

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
        return SecurityUtils.getSubject().hasRole("admin");
    }

    @Override
    public int getUserId() {
        return SecurityUtils.getSubject().getPrincipals().oneByType(CodeDefendersRealm.LocalUserId.class).getUserId();
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
