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

package org.codedefenders.auth;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.servlets.auth.CodeDefendersFormAuthenticationFilter;

/**
 * Implements a Realm that uses the {@link UserRepository} for authenticating users. The logic
 * to record the start of a session and the like has been moved inside the
 * {@link CodeDefendersFormAuthenticationFilter#onLoginSuccess} method
 *
 * @author gambi
 *
 */
@Singleton
public class CodeDefendersAuthenticatingRealm extends AuthenticatingRealm {

    @Inject
    SettingsRepository settingsRepository;

    @Inject
    UserRepository userRepository;

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
            throws AuthenticationException {

        if (token instanceof UsernamePasswordToken) {
            UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;

            UserEntity activeUser = userRepository.getUserByName(usernamePasswordToken.getUsername());

            if (activeUser == null) {
                throw new UnknownAccountException("Username not found or password incorrect.");
            }

            if (settingsRepository.isMailValidationRequired() && !activeUser.isValidated()) {
                throw new LockedAccountException("Account email is not validated.");
            }

            if (!activeUser.isActive()) {
                throw new LockedAccountException(
                        "Your account is inactive, login is only possible with an active account.");
            }

            String dbPassword = activeUser.getEncodedPassword();

            if (UserEntity.passwordMatches(new String(usernamePasswordToken.getPassword()), dbPassword)) {
                return new SimpleAuthenticationInfo(activeUser, usernamePasswordToken.getPassword(), getName());
            } else {
                throw new IncorrectCredentialsException("Username not found or password incorrect.");
            }
        } else {
            throw new UnsupportedTokenException("We don't support the provided token");
        }
    }
}
