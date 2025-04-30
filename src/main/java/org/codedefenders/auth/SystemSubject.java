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

import java.util.concurrent.Callable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.mgt.WebSecurityManager;

@ApplicationScoped
public class SystemSubject {
    private static Subject instance;

    private final CodeDefendersRealm realm;
    private final WebSecurityManager securityManager;

    @Inject
    public SystemSubject(CodeDefendersRealm realm, WebSecurityManager securityManager) {
        this.realm = realm;
        this.securityManager = securityManager;
    }

    public Subject get() {
        if (instance == null) {
            var context = new DefaultSubjectContext();
            context.setPrincipals(new SimplePrincipalCollection(
                    new CodeDefendersRealm.SystemPrincipal(), realm.getName()));
            context.setAuthenticated(true);
            instance = securityManager.createSubject(context);
        }
        return instance;
    }

    public <V> V execute(Callable<V> callable) {
        return get().execute(callable);
    }

    public void execute(Runnable runnable) {
        get().execute(runnable);
    }

    public <V> Callable<V> associateWith(Callable<V> callable) {
        return get().associateWith(callable);
    }

    public Runnable associateWith(Runnable runnable) {
        return get().associateWith(runnable);
    }
}
