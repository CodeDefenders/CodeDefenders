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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.shiro.authc.Account;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codedefenders.auth.permissions.AdminPermission;
import org.codedefenders.auth.permissions.CreateClassroomPermission;
import org.codedefenders.auth.roles.AdminRole;
import org.codedefenders.auth.roles.AuthRole;
import org.codedefenders.auth.roles.SystemRole;
import org.codedefenders.auth.roles.TeacherRole;
import org.codedefenders.auth.roles.UserRole;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.RoleService;
import org.codedefenders.servlets.auth.CodeDefendersFormAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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

    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final RoleService roleService;

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

    public class CodeDefendersPermissionResolver implements PermissionResolver {
        @Override
        public Permission resolvePermission(String name) {
            return CodeDefendersRealm.this.resolvePermission(name);
        }
    }

    public class CodeDefendersRolePermissionResolver implements RolePermissionResolver {
        @Override
        public Collection<Permission> resolvePermissionsInRole(String name) {
            return resolveRole(name).getPermissions();
        }
    }

    @Inject
    public CodeDefendersRealm(CodeDefendersCacheManager codeDefendersCacheManager,
                              CodeDefendersCredentialsMatcher codeDefendersCredentialsMatcher, SettingsRepository settingsRepo,
                              UserRepository userRepo, RoleService roleService) {
        super(codeDefendersCacheManager, codeDefendersCredentialsMatcher);
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.roleService = roleService;

        this.setCachingEnabled(true);
        this.setAuthenticationCachingEnabled(true);

        this.setPermissionResolver(new CodeDefendersPermissionResolver());
        this.setRolePermissionResolver(new CodeDefendersRolePermissionResolver());
    }

    protected Account getAccount(UserEntity userEntity) {

        Set<String> roleNames = new HashSet<>();

        // imply user role for all users
        AuthRole userRole = new UserRole();
        roleNames.add(userRole.getName());

        // get other roles from db
        for (AuthRole role : roleService.getRolesForUser(userEntity.getId())) {
            roleNames.add(role.getName());
        }

        Collection<Object> principals = List.of(new LocalUserId(userEntity.getId()));
        return new SimpleAccount(principals, userEntity.getEncodedPassword(), getName(), roleNames, Set.of());
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
        var localUserId = principals.oneByType(LocalUserId.class);
        if (localUserId != null) {
            return localUserId.getUserId();
        }

        var systemPrincipal = principals.oneByType(SystemPrincipal.class);
        if (systemPrincipal != null) {
            return systemPrincipal;
        }

        throw new AuthorizationException("No known principals.");
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
     * See: {@link #getAuthorizationInfo(PrincipalCollection)}
     * See: {@link #clearCachedAuthorizationInfo(PrincipalCollection)}
     * See: {@link #clearCache(PrincipalCollection)}
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        LocalUserId localUserId =  principalCollection.oneByType(LocalUserId.class);
        SystemPrincipal systemPrincipal = principalCollection.oneByType(SystemPrincipal.class);

        var user = Optional.ofNullable(localUserId)
                        .map(LocalUserId::getUserId)
                        .flatMap(userRepo::getUserById);

        if (user.isPresent()) {
            return getAccount(user.get());
        } else if (systemPrincipal != null) {
            return new SimpleAuthorizationInfo(Set.of(SystemRole.name));
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

    public static class SystemPrincipal implements Serializable {
    }

    public AuthRole resolveRole(String name) {
        return switch (name) {
            case UserRole.name -> new UserRole();
            case TeacherRole.name -> new TeacherRole();
            case AdminRole.name -> new AdminRole();
            case SystemRole.name -> new SystemRole();
            default -> throw new AuthorizationException("Unknown role: '" + name + "'.");
        };
    }

    public Permission resolvePermission(String name) {
        return switch (name) {
            case AdminPermission.name -> new AdminPermission();
            case CreateClassroomPermission.name -> new CreateClassroomPermission();
            default -> throw new AuthorizationException("Unknown permission: '" + name + "'.");
        };
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
