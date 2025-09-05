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
package org.codedefenders.util;

/**
 * This class contains URL path constants.
 *
 * <p>If one path has to be adjusted, it has to be adjusted in the {@code web.xml}
 * servlet mapping configuration, too.
 */
public class Paths {

    public static final String[] STATIC_RESOURCE_PREFIXES = {"/js", "/css", "/images", "/webjars", "/favicon.ico"};

    // URL Paths
    public static final String LANDING_PAGE = "/";
    public static final String LOGIN = "/login";
    public static final String LOGOUT = "/logout";
    public static final String HELP_PAGE = "/help";
    public static final String ABOUT_PAGE = "/about";
    public static final String CONTACT_PAGE = "/contact";
    public static final String LEADERBOARD_PAGE = "/leaderboard";
    public static final String IMPRINT_PAGE = "/imprint";

    // TODO This might be duplicated. But then, why there's a profile
    public static final String USER = "/user";
    public static final String USER_PROFILE = "/profile";
    public static final String USER_SETTINGS = "/account-settings";

    public static final String PASSWORD = "/password";


    public static final String GAMES_OVERVIEW = "/games/overview";
    public static final String GAMES_HISTORY = "/games/history";
    public static final String CLASS_UPLOAD = "/class-upload";

    public static final String PROJECT_EXPORT = "/project-export";
    public static final String PUZZLE_IMPORT = "/puzzle-import";

    public static final String EQUIVALENCE_DUELS_GAME = "/equivalence-duels";

    public static final String BATTLEGROUND_GAME = "/multiplayergame";
    public static final String BATTLEGROUND_HISTORY = "/multiplayer/history";
    public static final String BATTLEGROUND_SELECTION = "/multiplayer/games";
    public static final String BATTLEGROUND_CREATE = "/multiplayer/create";

    public static final String MELEE_GAME = "/meleegame";
    public static final String MELEE_HISTORY = "/meleegame/history";
    public static final String MELEE_SELECTION = "/melee/games";
    public static final String MELEE_CREATE = "/melee/create";

    public static final String PUZZLE_OVERVIEW = "/puzzles";
    public static final String PUZZLE_GAME = "/puzzlegame";

    public static final String CLASSROOM = "/classroom";
    public static final String CLASSROOMS_OVERVIEW = "/classrooms";
    public static final String CLASSROOM_CREATE_GAMES = "/classroom/creategames";

    public static final String INVITE = "/invite";


    public static final String ADMIN_PAGE = "/admin";
    public static final String ADMIN_GAMES = "/admin/games";
    public static final String ADMIN_MONITOR = "/admin/monitor";
    public static final String ADMIN_CLASSES = "/admin/classes";
    public static final String ADMIN_USERS = "/admin/users";
    public static final String ADMIN_SETTINGS = "/admin/settings";
    public static final String ADMIN_KILLMAPS = "/admin/killmaps";
    public static final String ADMIN_CLASSROOMS = "/admin/classrooms";

    public static final String ADMIN_PUZZLE_OVERVIEW = "/admin/puzzles";
    public static final String ADMIN_PUZZLE_MANAGEMENT = "/admin/puzzles/management";
    public static final String ADMIN_PUZZLE_UPLOAD = "/admin/puzzles/upload";

    public static final String ADMIN_ANALYTICS_USERS = "/admin/analytics/users";
    public static final String ADMIN_ANALYTICS_CLASSES = "/admin/analytics/classes";
    public static final String ADMIN_ANALYTICS_KILLMAPS = "/admin/analytics/killmaps";

    public static final String API_FEEDBACK = "/api/feedback";
    public static final String API_SEND_EMAIL = "/api/sendmail";
    public static final String API_CLASS = "/api/class";
    public static final String API_TEST = "/api/test";
    public static final String API_MUTANT = "/api/mutant";
    public static final String API_GAME_CHAT = "/api/game-chat";
    public static final String API_CLASSROOM = "/api/classroom";
    public static final String API_MESSAGES = "/api/messages";

    public static final String API_ANALYTICS_USERS = "/admin/api/users";
    public static final String API_ANALYTICS_CLASSES = "/admin/api/classes";
    public static final String API_ANALYTICS_KILLMAP = "/admin/api/killmap";
    public static final String API_KILLMAP_MANAGEMENT = "/admin/api/killmapmanagement";
    public static final String API_ADMIN_PUZZLES_ALL = "/admin/api/puzzles";
    public static final String API_ADMIN_PUZZLE = "/admin/api/puzzles/puzzle";
    public static final String API_ADMIN_PUZZLECHAPTER = "/admin/api/puzzles/chapter";
    public static final String WHITELIST_API = "/api/whitelist";
}
