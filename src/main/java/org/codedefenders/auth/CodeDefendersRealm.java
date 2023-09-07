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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.shiro.authc.Account;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.servlets.auth.CodeDefendersFormAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.cache.CacheBuilder;

/**
 * Implements a Realm that uses the {@link UserRepository} for authenticating users. The logic
 * to record the start of a session and the like has been moved inside the
 * {@link CodeDefendersFormAuthenticationFilter#onLoginSuccess} method
 *
 * @author gambi
 */
@Singleton
public class CodeDefendersRealm extends AuthorizingRealm {
    private static final Logger logger = LoggerFactory.getLogger(CodeDefendersRealm.class);

    private final String adminRole;

    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;

    private final UserDatabase userDatabase;

    public static class CodeDefendersCacheManager extends AbstractCacheManager {
        private final MetricsRegistry metricsRegistry;

        @Inject
        public CodeDefendersCacheManager(MetricsRegistry metricsRegistry) {
            this.metricsRegistry = metricsRegistry;
        }

        @Override
        protected Cache<Object, Object> createCache(String s) throws CacheException {
            com.google.common.cache.Cache<Object, Object> cache = CacheBuilder.newBuilder()
                    .recordStats()
                    .build();

            metricsRegistry.registerGuavaCache("shiroCache_" + s, cache);

            return new GuavaCache(cache);
        }
    }

    public static class CodeDefendersCredentialsMatcher implements CredentialsMatcher {
        private final PasswordEncoder passwordEncoder;

        @Inject
        public CodeDefendersCredentialsMatcher(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        public boolean doCredentialsMatch(AuthenticationToken authenticationToken,
                AuthenticationInfo authenticationInfo) {
            return passwordEncoder.matches(
                    // authenticationToken.getCredentials returns char array so convert to String
                    new String((char[]) authenticationToken.getCredentials()),
                    (String) authenticationInfo.getCredentials()
            );
        }
    }

    @Inject
    public CodeDefendersRealm(CodeDefendersCacheManager codeDefendersCacheManager,
            CodeDefendersCredentialsMatcher codeDefendersCredentialsMatcher, SettingsRepository settingsRepo,
            UserRepository userRepo, @SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        super(codeDefendersCacheManager, codeDefendersCredentialsMatcher);
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;

        UserDatabase userDatabase;
        try {
            userDatabase = (UserDatabase) new InitialContext().lookup("java:comp/env/auth/UserDatabase");
        } catch (NamingException e) {
            logger.error("Exception looking up user database", e);
            userDatabase = null;
            // TODO(Alex): Should we really continue here?!
        }
        this.userDatabase = userDatabase;

        this.adminRole = config.getAuthAdminRole();

        this.setCachingEnabled(true);
        this.setAuthenticationCachingEnabled(true);
    }

    protected Account getAccount(UserEntity userEntity) {

        Collection<Object> principals = new ArrayList<>();
        principals.add(new LocalUserId(userEntity.getId()));

        Set<String> roles = new java.util.HashSet<>();
        roles.add("user");

        User tomcatUser = userDatabase.findUser(userEntity.getUsername());
        if (tomcatUser != null && tomcatUser.isInRole(userDatabase.findRole(adminRole))) {
            roles.add("admin");
        }

        return new SimpleAccount(principals, userEntity.getEncodedPassword(), getName(), roles, new HashSet<>());
    }

    @Override
    protected Object getAuthenticationCacheKey(AuthenticationToken token) {
        Optional<UserEntity> user = userRepo.getUserByName((String) token.getPrincipal());
        if (user.isPresent()) {
            return user.get().getId();
        } else {
            return null;
        }
    }

    @Override
    protected Object getAuthenticationCacheKey(PrincipalCollection principals) {
        return principals.oneByType(LocalUserId.class).getUserId();
    }

    @Override
    protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
        return principals.oneByType(LocalUserId.class).getUserId();
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
            throws AuthenticationException {

        if (token instanceof UsernamePasswordToken usernamePasswordToken) {

            Optional<UserEntity> activeUser = userRepo.getUserByName(usernamePasswordToken.getUsername());

            if (activeUser.isPresent()) {
                UserEntity user = activeUser.get();

                if (settingsRepo.isMailValidationRequired() && !user.isValidated()) {
                    throw new LockedAccountException("Account email is not validated.");
                }

                if (!user.isActive()) {
                    throw new LockedAccountException(
                            "Your account is inactive, login is only possible with an active account.");
                }

                // Note: The password matching is done by our custom CredentialsMatcher
                // {@link CodeDefendersCredentialsMatcher} setup in the constructor.
                return getAccount(user);
            } else {
                return null;
            }
        } else {
            // TODO(Alex): Why not a Runtime exception? This case shouldn't happen!!
            throw new UnsupportedTokenException("We don't support the provided token");
        }
    }

    public void invalidate(int userId) {

        Set<Object> principals = new HashSet<>();
        principals.add(new LocalUserId(userId));

        clearCache(new SimplePrincipalCollection(principals, getName()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>TODO(Alex): Implement Cache invalidation (If roles are tracked in the Database);
     *
     * See: {@link #getAuthorizationInfo(PrincipalCollection)}
     * See: {@link #clearCachedAuthorizationInfo(PrincipalCollection)}
     * See: {@link #clearCache(PrincipalCollection)}
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        Optional<UserEntity> user = userRepo.getUserById(principalCollection.oneByType(LocalUserId.class).getUserId());

        if (user.isPresent()) {
            return getAccount(user.get());
        } else {
            // Something weird happened
            // TODO(Alex): Better Error handling
            throw new IllegalStateException("This shouldn't happen");
        }
    }

    public static class LocalUserId implements Serializable {
        private final int userId;

        public LocalUserId(int userId) {
            this.userId = userId;
        }

        public int getUserId() {
            return userId;
        }
    }

    /**
     * Simple adapter for using {@link com.google.common.cache.Cache} as a {@link org.apache.shiro.cache.Cache}.
     */
    private static class GuavaCache implements Cache<Object, Object> {
        com.google.common.cache.Cache<Object, Object> backingCache;

        public GuavaCache(@Nonnull com.google.common.cache.Cache<Object, Object> backingCache) {
            this.backingCache = backingCache;
        }

        @Override
        public Object get(Object o) throws CacheException {
            if (o == null) {
                return null;
            } else {
                return backingCache.getIfPresent(o);
            }
        }

        @Override
        public synchronized Object put(Object o, Object o2) throws CacheException {
            Object previous;

            previous = get(o);
            if (previous == o2) {
                return null;
            } else {
                backingCache.put(o, o2);
                return previous;
            }
        }

        @Override
        public synchronized Object remove(Object o) throws CacheException {
            Object value = get(o);
            backingCache.invalidate(o);
            return value;
        }

        @Override
        public void clear() throws CacheException {
            backingCache.invalidateAll();
        }

        @Override
        public int size() {
            return (int) backingCache.size();
        }

        @Override
        public Set<Object> keys() {
            return backingCache.asMap().keySet();
        }

        @Override
        public Collection<Object> values() {
            return backingCache.asMap().values();
        }
    }
}
