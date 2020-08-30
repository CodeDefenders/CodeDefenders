package org.codedefenders.servlets.auth;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
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

//            SimpleAccountRealm realm = new SimpleAccountRealm();
//            realm.addAccount("creator", "12345678");
//            realm.addAccount("attacker", "12345678");
//            realm.addAccount("defender", "12345678");

//            System.out.println("\n\n\n");
//            System.out.println("CodeDefendersHelper.getSecurityManager() Initialize SecurityManager");
//            System.out.println("\n\n\n");
            /*
             * Get the BackendExecutorService since dependency injection does not work on
             * this class.
             */
//            CodeDefendersAuthenticatingRealm realm = null;
//            try {
//                InitialContext initialContext = new InitialContext();
//                BeanManager bm = (BeanManager) initialContext.lookup("java:comp/env/BeanManager");
//                Bean bean = (Bean) bm.getBeans(CodeDefendersAuthenticatingRealm.class).iterator().next();
//                CreationalContext ctx = bm.createCreationalContext(bean);
//                realm = (CodeDefendersAuthenticatingRealm) bm.getReference(bean, CodeDefendersAuthenticatingRealm.class,
//                        ctx);
//            } catch (NamingException e) {
//                e.printStackTrace();
//                System.out.println("CodeDefendersHelper.getSecurityManager() Could not acquire BeanManager");
//            }
//            /*
//             * If we are running this outside a container, DI must be done manually by
//             * looking up the JNDI resource.
//             */
//            if (realm == null) {
//                try {
//                    InitialContext initialContext = new InitialContext();
//                    realm = (CodeDefendersAuthenticatingRealm) initialContext
//                            .lookup("java:comp/env/codedefenders/realm");
//                } catch (NamingException e) {
//                    e.printStackTrace();
//                    System.out.println(
//                            "CodeDefendersHelper.getSecurityManager() Could not acquire BeanManager outside CDI");
//                }
//            }
//
//            if (realm != null) {
//                securityManager = new DefaultWebSecurityManager(realm);
//            } else {
//                throw new RuntimeException("Cannot Initialize security ! Realm is null");
//            }
//            
//            System.out.println("\n\n\n");
//            System.out.println("CodeDefendersHelper.getSecurityManager() Initialized SecurityManager");
//            System.out.println("\n\n\n");
            CodeDefendersAuthenticatingRealm realm = new CodeDefendersAuthenticatingRealm();
            
            securityManager = new DefaultWebSecurityManager(realm);
            // TODO Not sure this is necessary at all...
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
            // Go to landing page after login unless another url has been asked
            authc.setSuccessUrl(org.codedefenders.util.Paths.GAMES_OVERVIEW);

            LogoutFilter logout = new LogoutFilter();

            FilterChainManager fcMan = new DefaultFilterChainManager();
            fcMan.addFilter("authc", authc);
            fcMan.addFilter("logout", logout);

            /*
             * TODO Ideally there are annotations that can be used, but those are not
             * automatically enacted, so for the moment we keep the logic here. TODO
             * Reorganize URL so they give a structure to the app and the various resources.
             */
            fcMan.createChain("/logout", "logout");

            // THIS FUCKERY !!!! WE NEED TO TELL THE FILTER TO PROCESS THIS REQUEST TOOOOO
            // !!!
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