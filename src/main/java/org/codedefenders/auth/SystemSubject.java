package org.codedefenders.auth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectCallable;
import org.apache.shiro.subject.support.SubjectRunnable;
import org.codedefenders.auth.roles.SystemRole;

public class SystemSubject implements Subject {

    private static final SystemPrincipal principal = new SystemPrincipal();
    private static final SystemSubject instance = new SystemSubject();

    public static SystemSubject get() {
        return instance;
    }

    public static class SystemPrincipal implements Serializable {
        public static final String name = "system";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public PrincipalCollection getPrincipals() {
        return new SimplePrincipalCollection(principal, CodeDefendersRealm.name);
    }

    @Override
    public boolean isPermitted(String permission) {
        return true;
    }

    @Override
    public boolean isPermitted(Permission permission) {
        return true;
    }

    @Override
    public boolean[] isPermitted(String... permissions) {
        return Arrays.stream(permissions)
                .map(this::isPermitted)
                .collect(new ToBoolArray());
    }

    @Override
    public boolean[] isPermitted(List<Permission> permissions) {
        return permissions.stream()
                .map(this::isPermitted)
                .collect(new ToBoolArray());
    }

    @Override
    public boolean isPermittedAll(String... permissions) {
        return true;
    }

    @Override
    public boolean isPermittedAll(Collection<Permission> permissions) {
        return true;
    }

    @Override
    public void checkPermission(String permission) throws AuthorizationException {
        // never throw exception
    }

    @Override
    public void checkPermission(Permission permission) throws AuthorizationException {
        // never throw exception
    }

    @Override
    public void checkPermissions(String... permissions) throws AuthorizationException {
        // never throw exception
    }

    @Override
    public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {
        // never throw exception
    }

    @Override
    public boolean hasRole(String roleIdentifier) {
        return roleIdentifier.equals(SystemRole.name);
    }

    @Override
    public boolean[] hasRoles(List<String> roleIdentifiers) {
        return roleIdentifiers.stream()
                .map(this::hasRole)
                .collect(new ToBoolArray());
    }

    @Override
    public boolean hasAllRoles(Collection<String> roleIdentifiers) {
        return roleIdentifiers.stream()
                .allMatch(this::hasRole);
    }

    @Override
    public void checkRole(String roleIdentifier) throws AuthorizationException {
        if (!hasRole(roleIdentifier)) {
            throw new UnauthorizedException("User does not have role [" + roleIdentifier + "]");
        }
    }

    @Override
    public void checkRoles(Collection<String> roleIdentifiers) throws AuthorizationException {
        for (String roleIdentifier : roleIdentifiers) {
            if (!hasRole(roleIdentifier)) {
                throw new UnauthorizedException("User does not have role [" + roleIdentifier + "]");
            }
        }
    }

    @Override
    public void checkRoles(String... roleIdentifiers) throws AuthorizationException {
        for (String roleIdentifier : roleIdentifiers) {
            if (!hasRole(roleIdentifier)) {
                throw new UnauthorizedException("User does not have role [" + roleIdentifier + "]");
            }
        }
    }

    @Override
    public void login(AuthenticationToken token) throws AuthenticationException {

    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public boolean isRemembered() {
        return true;
    }

    @Override
    public Session getSession() {
        return null;
    }

    @Override
    public Session getSession(boolean create) {
        return null;
    }

    @Override
    public void logout() {

    }

    @Override
    public <V> V execute(Callable<V> callable) throws ExecutionException {
        try {
            return associateWith(callable).call();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void execute(Runnable runnable) {
        try {
            associateWith(runnable).run();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public <V> Callable<V> associateWith(Callable<V> callable) {
        return new SubjectCallable<>(this, callable);
    }

    @Override
    public Runnable associateWith(Runnable runnable) {
        return new SubjectRunnable(this, runnable);
    }

    @Override
    public void runAs(PrincipalCollection principals) throws NullPointerException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRunAs() {
        return false;
    }

    @Override
    public PrincipalCollection getPreviousPrincipals() {
        return null;
    }

    @Override
    public PrincipalCollection releaseRunAs() {
        return null;
    }

    private static class ToBoolArray implements Collector<Boolean, List<Boolean>, boolean[]> {
        @Override
        public Supplier<List<Boolean>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<Boolean>, Boolean> accumulator() {
            return List::add;
        }

        @Override
        public BinaryOperator<List<Boolean>> combiner() {
            return (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            };
        }

        @Override
        public Function<List<Boolean>, boolean[]> finisher() {
            return list -> {
                boolean[] result = new boolean[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    result[i] = list.get(i);
                }
                return result;
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }
}

