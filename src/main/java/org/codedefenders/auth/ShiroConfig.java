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

import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.codedefenders.servlets.auth.CodeDefendersFormAuthenticationFilter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * This class configures Shiro.
 *
 * @author gambi
 */
@ApplicationScoped
public class ShiroConfig {

    @Produces
    @Singleton
    public WebSecurityManager getSecurityManager(CodeDefendersRealm authenticatingRealm) {
        return new DefaultWebSecurityManager(authenticatingRealm);
    }

    /**
     * Configure the filtering mechanism. This might be replaced by annotations only
     * if we find a way to let the tomcat process them. Otherwise, we can use some
     * other configuration means
     */
    @Produces
    @Singleton
    public FilterChainResolver getFilterChainResolver(CodeDefendersFormAuthenticationFilter authc) {
        /*
         * This filter uses the form data to check the user given the configured realms
         */

        LogoutFilter logout = new LogoutFilter();
        // org.codedefenders.util.Paths.LOGOUT = "/logout";
        logout.setRedirectUrl(org.codedefenders.util.Paths.LANDING_PAGE);

        FilterChainManager fcMan = new DefaultFilterChainManager();
        fcMan.addFilter("logout", logout);
        fcMan.addFilter("authc", authc);
        // Additional 'default' filter e.g. `roles[…]` are also available


        // Logout Chain
        fcMan.createChain(org.codedefenders.util.Paths.LOGOUT, "logout");

        /*
         * Note: this is necessary to make sure authc is applied also to login page,
         * otherwise the Authentication filter will not be invoked
         */
        fcMan.createChain("/login", "authc");
        /*
         * TODO This list might be incomplete.
         */
        // Public URLs - Does not require authentication
        // org.codedefenders.util.Paths.HELP_PAGE = "/help";
        // org.codedefenders.util.Paths.ABOUT_PAGE = "/about";
        // org.codedefenders.util.Paths.CONTACT_PAGE = "/contact";
        // org.codedefenders.util.Paths.LEADERBOARD_PAGE = "/leaderboard";

        // "Semi"-public.
        // org.codedefenders.util.Paths.USER = "/user";
        // fcMan.createChain(org.codedefenders.util.Paths.USER, AUTHENTICATION);
        // org.codedefenders.util.Paths.USER_PROFILE = "/profile";


        // URLs that require authentication

        fcMan.createChain(org.codedefenders.util.Paths.USER_SETTINGS, "authc");

        // org.codedefenders.util.Paths.GAMES_OVERVIEW = "/games/overview";
        // org.codedefenders.util.Paths.GAMES_HISTORY = "/games/history";
        fcMan.createChain("/games/**", "authc"); // Not refactory safe

        // org.codedefenders.util.Paths.CLASS_UPLOAD = "/class-upload";
        fcMan.createChain(org.codedefenders.util.Paths.CLASS_UPLOAD, "authc");

        // org.codedefenders.util.Paths.PROJECT_EXPORT = "/project-export";
        fcMan.createChain(org.codedefenders.util.Paths.PROJECT_EXPORT, "authc");

        // org.codedefenders.util.Paths.EQUIVALENCE_DUELS_GAME = "/equivalence-duels";
        fcMan.createChain(org.codedefenders.util.Paths.EQUIVALENCE_DUELS_GAME, "authc");

        // org.codedefenders.util.Paths.BATTLEGROUND_GAME = "/multiplayergame";
        // org.codedefenders.util.Paths.BATTLEGROUND_HISTORY = "/multiplayer/history";
        // org.codedefenders.util.Paths.BATTLEGROUND_SELECTION = "/multiplayer/games";
        // org.codedefenders.util.Paths.BATTLEGROUND_CREATE = "/multiplayer/create";
        fcMan.createChain("/multiplayergame/**", "authc");
        fcMan.createChain("/multiplayer/**", "authc");

        // org.codedefenders.util.Paths.MELEE_GAME = "/meleegame";
        // org.codedefenders.util.Paths.MELEE_HISTORY = "/meleegame/history";
        // org.codedefenders.util.Paths.MELEE_SELECTION = "/melee/games";
        // org.codedefenders.util.Paths.MELEE_CREATE = "/melee/create";
        fcMan.createChain("/meleegame/**", "authc");
        fcMan.createChain("/melee/**", "authc");

        // TODO Refactor URL as games/puzzle/** or at least "/puzzle/**"
        // org.codedefenders.util.Paths.PUZZLE_OVERVIEW = "/puzzles";
        // org.codedefenders.util.Paths.PUZZLE_GAME = "/puzzlegame";
        // org.codedefenders.util.Paths.PUZZLE_GAME_SELECTION = "/puzzle/games";
        fcMan.createChain("/puzzle**", "authc");

        // API URLs. I assume they require authentication, but I might be wrong. Maybe
        // they require a different type of authentication, e.g., if the are REST
        // org.codedefenders.util.Paths.API_MESSAGES = "/api/messages"; // path used in messaging.js
        // org.codedefenders.util.Paths.API_FEEDBACK = "/api/feedback";
        // org.codedefenders.util.Paths.API_SEND_EMAIL = "/api/sendmail";
        // org.codedefenders.util.Paths.API_CLASS = "/api/class";
        // org.codedefenders.util.Paths.API_TEST = "/api/test";
        // org.codedefenders.util.Paths.API_MUTANT = "/api/mutant";
        fcMan.createChain("/api/**", "authc");

        // Admin URLS. This does not necessary require authentication as we handle that
        // using tomcat. But for completeness, we force it now.
        // Later we will also add the Admin role
        // org.codedefenders.util.Paths.ADMIN_PAGE = "/admin";
        // org.codedefenders.util.Paths.ADMIN_GAMES = "/admin/games";
        // org.codedefenders.util.Paths.ADMIN_MONITOR = "/admin/monitor";
        // org.codedefenders.util.Paths.ADMIN_CLASSES = "/admin/classes";
        // org.codedefenders.util.Paths.ADMIN_USERS = "/admin/users";
        // org.codedefenders.util.Paths.ADMIN_SETTINGS = "/admin/settings";
        // org.codedefenders.util.Paths.ADMIN_KILLMAPS = "/admin/killmaps";

        // org.codedefenders.util.Paths.ADMIN_PUZZLE_OVERVIEW = "/admin/puzzles";
        // org.codedefenders.util.Paths.ADMIN_PUZZLE_MANAGEMENT = "/admin/puzzles/management";
        // org.codedefenders.util.Paths.ADMIN_PUZZLE_UPLOAD = "/admin/puzzles/upload";

        // org.codedefenders.util.Paths.ADMIN_ANALYTICS_USERS = "/admin/analytics/users";
        // org.codedefenders.util.Paths.ADMIN_ANALYTICS_CLASSES = "/admin/analytics/classes";
        // org.codedefenders.util.Paths.ADMIN_ANALYTICS_KILLMAPS = "/admin/analytics/killmaps";

        // org.codedefenders.util.Paths.API_ANALYTICS_USERS = "/admin/api/users";
        // org.codedefenders.util.Paths.API_ANALYTICS_CLASSES = "/admin/api/classes";
        // org.codedefenders.util.Paths.API_ANALYTICS_KILLMAP = "/admin/api/killmap";
        // org.codedefenders.util.Paths.API_KILLMAP_MANAGEMENT = "/admin/api/killmapmanagement";
        // org.codedefenders.util.Paths.API_ADMIN_PUZZLES_ALL = "/admin/api/puzzles";
        // org.codedefenders.util.Paths.API_ADMIN_PUZZLE = "/admin/api/puzzles/puzzle";
        // org.codedefenders.util.Paths.API_ADMIN_PUZZLECHAPTER = "/admin/api/puzzles/chapter";
        fcMan.createChain("/admin/**", "authc, roles[admin]");

        fcMan.createChain("/classroom/**", "authc");
        fcMan.createChain("/classrooms", "authc");

        PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
        resolver.setFilterChainManager(fcMan);
        return resolver;
    }

}
