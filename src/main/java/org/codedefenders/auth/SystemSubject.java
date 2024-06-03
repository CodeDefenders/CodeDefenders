package org.codedefenders.auth;

import java.util.concurrent.Callable;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.mgt.WebSecurityManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
