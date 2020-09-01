package org.codedefenders.servlets.auth;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

/**
 * Shiro and WELD conflicts becasue both needs to handle object lifecycle, so we
 * pretend we do the injection here by looking up our custom Realm from JNDI
 * 
 * @author gambi
 *
 */
public class CodeDefendersHelper {

    private static SecurityManager securityManager = null;
    private static FilterChainResolver filterChainResolver = null;

    public static SecurityManager getSecurityManager() {

        if (securityManager == null) {
            CodeDefendersAuthenticatingRealm realm = new CodeDefendersAuthenticatingRealm();
            securityManager = new DefaultWebSecurityManager(realm);

            CacheManager cacheManager = new MemoryConstrainedCacheManager();
            ((DefaultWebSecurityManager) securityManager).setCacheManager(cacheManager);

        }
        return securityManager;
    }

    /**
     * Configure the filtering mechanism. This might be replaced by annotations only
     * if we find a way to let the tomcat process them. Otherwise, we can use some
     * other configuration means
     * 
     * @return
     */
    public static FilterChainResolver getFilterChainResolver() {
        if (filterChainResolver == null) {

            /*
             * This filter uses the form data to check the user given the configured realms
             */
            FormAuthenticationFilter authc = new CodeDefendersFormAuthenticationFilter();
            // URL for the login. This tells shiro filter to let the request flow without
            // any additional check
            authc.setLoginUrl("/login");
            // Go to game overview page after successful login
            authc.setSuccessUrl(org.codedefenders.util.Paths.GAMES_OVERVIEW);

            LogoutFilter logout = new LogoutFilter();
            logout.setRedirectUrl(org.codedefenders.util.Paths.LANDING_PAGE);

            FilterChainManager fcMan = new DefaultFilterChainManager();
            fcMan.addFilter("authc", authc);
            fcMan.addFilter("logout", logout);
            
            fcMan.createChain("/logout", "logout");

            /*
             * Note: this is necessary to make sure authc is applied also to login page,
             * otherwise the Authentication filter will not be invoked
             */
            fcMan.createChain("/login", "authc");
            fcMan.createChain("/games/**", "authc");
            fcMan.createChain("/multiplayer/**", "authc");
            fcMan.createChain("/multiplayergame/**", "authc");
            fcMan.createChain("/melee/**", "authc");
            fcMan.createChain("/meleegame/**", "authc");
            fcMan.createChain("/puzzle/**", "authc");
            fcMan.createChain("/puzzlegame/**", "authc");
            fcMan.createChain("/class-upload", "authc");
            // Secure + Admin
            fcMan.createChain("/admin/**", "authc");

            PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
            resolver.setFilterChainManager(fcMan);
            filterChainResolver = resolver;
        }
        return filterChainResolver;
    }

}