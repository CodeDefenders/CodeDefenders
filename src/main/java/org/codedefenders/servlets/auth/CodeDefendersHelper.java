package org.codedefenders.servlets.auth;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.apache.shiro.web.filter.authc.PassThruAuthenticationFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import com.mysql.cj.Constants;

// THIS Most likely needs to be replaced by CDI resources?
public class CodeDefendersHelper {

    private static SecurityManager securityManager = null;
    private static FilterChainResolver filterChainResolver = null;

    public static SecurityManager getSecurityManager() {
        if (securityManager == null) {
            // Here we should use the JDBC thingy or a Custom AuthorizingRealm that uses our
            // DAOs
            SimpleAccountRealm realm = new SimpleAccountRealm();
            realm.addAccount("creator", "12345678");
            realm.addAccount("attacker", "12345678");
            realm.addAccount("defender", "12345678");
            realm.addAccount("attacker2", "12345678");
            realm.addAccount("defender2", "12345678");

            securityManager = new DefaultWebSecurityManager(realm);
        }
        return securityManager;
    }

    public static FilterChainResolver getFilterChainResolver() {
        if (filterChainResolver == null) {
            // This delegates the logic to set the current use to someone else
            //
            // Does not seem to use follow-redirect?
            PassThruAuthenticationFilter authc = new PassThruAuthenticationFilter();
            // URL for the login. This tells shiro filter to let the request flow without
            // any additional check
            authc.setLoginUrl("/login");
            // Go to landing page
            authc.setSuccessUrl(org.codedefenders.util.Paths.GAMES_OVERVIEW);

            LogoutFilter logout = new LogoutFilter();

            // This might require that NO user is logged ?
//            AnonymousFilter anon = new AnonymousFilter();

            FilterChainManager fcMan = new DefaultFilterChainManager();
//            fcMan.addFilter("anon", anon);
            fcMan.addFilter("authc", authc);
            fcMan.addFilter("logout", logout);

            // Public
//            fcMan.createChain("/about", "logout");

            fcMan.createChain("/logout", "logout");
            // Authenticated + Player
            fcMan.createChain("/games/**", "authc");
            fcMan.createChain("/class-upload","authc"); 
            // Secure + Admin
            fcMan.createChain("/admin/**", "authc");

            PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
            resolver.setFilterChainManager(fcMan);
            filterChainResolver = resolver;
        }
        return filterChainResolver;
    }

}