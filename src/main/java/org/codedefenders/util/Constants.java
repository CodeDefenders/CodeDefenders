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

import java.nio.file.Paths;

import org.xnap.commons.i18n.I18n;

/**
 * This class contains most constants used in Code Defenders.
 * For URL paths, see {@link org.codedefenders.util.Paths}.
 *
 * @see org.codedefenders.util.Paths
 */
public class Constants {
    // Dummy game
    public static final int DUMMY_GAME_ID = -1;

    // Dummy user IDs
    public static final int DUMMY_CREATOR_USER_ID = -1;
    public static final int DUMMY_ATTACKER_USER_ID = 3;
    public static final int DUMMY_DEFENDER_USER_ID = 4;

    // classes, dependencies, mutants and tests subdirectories for CUTs
    public static final String CUTS_MUTANTS_DIR = "mutants";
    public static final String CUTS_TESTS_DIR = "tests";
    public static final String CUTS_CLASSES_DIR = "classes";

    // Puzzle and Battleground mutant and test folders
    public static final String MODE_PUZZLE_DIR = "puzzle";
    public static final String MODE_BATTLEGROUND_DIR = "mp";

    public static final String TEST_PREFIX = "Test";
    public static final String JAVA_SOURCE_EXT = ".java";
    public static final String JAVA_CLASS_EXT = ".class";
    public static final String TEST_INFO_EXT = ".xml";
    public static final String SUITE_EXT = "_ESTest";

    public static final String GRACE_PERIOD_MESSAGE = I18n.marktr("Game is now in grace period.");

    // JSP file paths
    public static final String INDEX_JSP = "/jsp/index.jsp";
    public static final String LOGIN_VIEW_JSP = Paths.get("jsp", "login_view.jsp").toString();
    public static final String UTESTING_VIEW_JSP = Paths.get("jsp", "utesting_view.jsp").toString();
    public static final String ERROR_403_JSP = Paths.get("jsp", "error_page_403.jsp").toString();
    public static final String ERROR_404_JSP = Paths.get("jsp", "error_page_404.jsp").toString();
    public static final String ERROR_500_JSP = Paths.get("jsp", "error_page_500.jsp").toString();

    public static final String USER_PROFILE_JSP = Paths.get("jsp", "user_profile.jsp").toString();
    public static final String USER_SETTINGS_JSP = Paths.get("jsp", "user_settings.jsp").toString();
    public static final String USER_NOT_FOUND_JSP = Paths.get("jsp", "error_page_user_not_found.jsp").toString();

    public static final String CLOSING_VIEW_JSP = "/jsp/closing_game_view.jsp";
    public static final String BATTLEGROUND_GAME_VIEW_JSP = "/jsp/battleground/game_view.jsp";
    public static final String BATTLEGROUND_DETAILS_VIEW_JSP = "/jsp/battleground/details_view.jsp";
    public static final String MELEE_GAME_VIEW_JSP = "/jsp/melee/game_view.jsp";
    public static final String MELEE_DETAILS_VIEW_JSP = "/jsp/melee/details_view.jsp";
    public static final String BATTLEGROUND_ROLE_SELECTION = "/jsp/battleground/role_selection.jsp";

    public static final String PUZZLE_OVERVIEW_VIEW_JSP = "/jsp/puzzle/puzzle_overview.jsp";
    public static final String PUZZLE_GAME_ATTACKER_VIEW_JSP = "/jsp/puzzle/attacker_view.jsp";
    public static final String PUZZLE_GAME_DEFENDER_VIEW_JSP = "/jsp/puzzle/defender_view.jsp";
    public static final String PUZZLE_GAME_EQUIVALENCE_VIEW_JSP = "/jsp/puzzle/equivalence_view.jsp";

    public static final String USER_GAMES_OVERVIEW_JSP = "/jsp/user_games_view.jsp";
    public static final String GAMES_HISTORY_JSP = "/jsp/games_history.jsp";
    public static final String CLASS_UPLOAD_VIEW_JSP = "/jsp/upload_class_view.jsp";

    public static final String ADMIN_USER_JSP = "/jsp/admin_user_mgmt.jsp";
    public static final String ADMIN_GAMES_JSP = "/jsp/admin_create_games.jsp";
    public static final String ADMIN_CLASSES_JSP = "/jsp/admin_class_management.jsp";
    public static final String ADMIN_SETTINGS_JSP = "/jsp/admin_system_settings.jsp";
    public static final String ADMIN_TEXT_SETTINGS_JSP = "/jsp/admin_text_settings.jsp";
    public static final String ADMIN_MONITOR_JSP = "/jsp/admin_monitor_games.jsp";
    public static final String ADMIN_KILLMAPS_JSP = "/jsp/admin_killmap_management.jsp";
    public static final String ADMIN_PUZZLE_MANAGEMENT_JSP = "/jsp/admin_puzzle_management.jsp";
    public static final String ADMIN_PUZZLE_PREVIEW_JSP = "/jsp/admin_puzzle_preview.jsp";
    public static final String ADMIN_ANALYTICS_USERS_JSP = "/jsp/admin_analytics_users.jsp";
    public static final String ADMIN_ANALYTICS_CLASSES_JSP = "/jsp/admin_analytics_classes.jsp";
    public static final String ADMIN_ANALYTICS_KILLMAPS_JSP = "/jsp/admin_analytics_killmaps.jsp";

    // Messages
    public static final String WINNER_MESSAGE = I18n.marktr("You won!");
    public static final String LOSER_MESSAGE = I18n.marktr("You lost!");
    public static final String DRAW_MESSAGE = I18n.marktr("It was a draw!");

    public static final String TEST_GENERIC_ERROR_MESSAGE = I18n.marktr("Sorry! An error on the server prevented the compilation of your test.");
    public static final String TEST_DID_NOT_COMPILE_MESSAGE = I18n.marktr("Your test did not compile. Try again, but with compilable code.");
    public static final String TEST_INVALID_MESSAGE = I18n.marktr("Your test is not valid. Remember the rules: Only one non-empty test, at most {0} assertions per test, no conditionals and no loops!");
    public static final String TEST_PASSED_ON_CUT_MESSAGE = I18n.marktr("Great! Your test compiled and passed on the original class under test.");
    public static final String TEST_DID_NOT_PASS_ON_CUT_MESSAGE = I18n.marktr("Your test did not pass on the original class under test. Try again.");
    public static final String TEST_KILLED_CLAIMED_MUTANT_MESSAGE = I18n.marktr("Yay, your test killed the allegedly equivalent mutant. You won the duel!");
    public static final String TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE = I18n.marktr("Oh no, your test did not kill the possibly equivalent mutant! You lost the duel.");
    public static final String TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE_KILLABLE = I18n.marktr("Oh no, your test did not kill the possibly equivalent mutant! You lost the duel. However, the mutant was killable!");
    public static final String TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE_KILLABLE_2 = I18n.marktr("Oh no, your test did not kill the possibly equivalent mutant! You lost the duel. Unfortunately, the mutant was killable!");
    public static final String TEST_SUBMITTED_MESSAGE = I18n.marktr("Test submitted and ready to kill mutants!");
    public static final String TEST_KILLED_ZERO_MESSAGE = I18n.marktr("Your test has not killed any mutants, just yet.");
    public static final String TEST_KILLED_LAST_MESSAGE = I18n.marktr("Great, your test killed the last mutant!");
    public static final String TEST_KILLED_ONE_MESSAGE = I18n.marktr("Great, your test killed a mutant!");
    public static final String TEST_KILLED_N_MESSAGE = I18n.marktr("Awesome! Your test killed {0} mutants!"); // number of mutants

    public static final String MUTANT_COMPILED_MESSAGE = I18n.marktr("Your mutant was compiled successfully.");
    public static final String MUTANT_ACCEPTED_EQUIVALENT_MESSAGE = I18n.marktr("The mutant was accepted as equivalent.");
    public static final String MUTANT_ACCEPTED_EQUIVALENT_MESSAGE_KILLABLE = I18n.marktr("The mutant was accepted as equivalent. However, the mutant was killable!");
    public static final String MUTANT_ACCEPTED_EQUIVALENT_MESSAGE_KILLABLE_VIEWABLE = I18n.marktr("The mutant was accepted as equivalent. However, the mutant was killable! You can view an example for a killing test in the mutant accordion.");
    public static final String MUTANT_UNCOMPILABLE_MESSAGE = I18n.marktr("Your mutant failed to compile. Try again.");
    public static final String MUTANT_INVALID_MESSAGE = I18n.marktr("Invalid mutant, sorry! Your mutant is identical to the CUT or it contains invalid code (ifs, loops, or new logical ops.)");
    public static final String MUTANT_CREATION_ERROR_MESSAGE = I18n.marktr("Oops! Something went wrong and the mutant was not created.");
    public static final String MUTANT_DUPLICATED_MESSAGE = I18n.marktr("Sorry, your mutant already exists in this game!");
    public static final String MUTANT_CLAIMED_EQUIVALENT_MESSAGE = I18n.marktr("Mutant claimed as equivalent, waiting for attacker to respond.");
    public static final String MUTANT_CLAIMED_EQUIVALENT_ERROR_MESSAGE = I18n.marktr("Something went wrong claiming an equivalent mutant"); // TODO: How?
    public static final String MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE = I18n.marktr("Cheeky! You cannot claim equivalence on untested lines!");
    public static final String MUTANT_KILLED_BY_TEST_MESSAGE = I18n.marktr("Test {0} killed your mutant. Better luck with the next one!"); // test
    public static final String MUTANT_SUBMITTED_MESSAGE = I18n.marktr("Mutant submitted, may the force be with it.");
    public static final String MUTANT_ALIVE_1_MESSAGE = I18n.marktr("Cool, your mutant survived its first test.");
    public static final String MUTANT_ALIVE_N_MESSAGE = I18n.marktr("Awesome, your mutant survived {0} tests!"); // number of tests that covered mutant

    public static final String MUTANT_MISSING_INTENTION = I18n.marktr("You must declare your intention.");

    // Message titles
    public static final String TITLE_SUCCESS = I18n.marktr("Success!");

    // Request attributes
    public static final String REQUEST_ATTRIBUTE_PUZZLE_GAME = "active_user_puzzle_game";

    public static final String ATTACKER_HAS_PENDING_DUELS = I18n.marktr("Sorry, your mutant cannot be accepted because you have pending equivalence duels!\nNo worries your mutant would be there ready to be submitted once you solve all your equivalence duels.");

    public static final String DEFAULT_KILL_MESSAGE = I18n.marktr("Sorry, no kill message available for this mutant");

    public static final String ILLEGAL_ACTION_MESSAGE = I18n.marktr("Hey! You're not allowed to do that!");
    //Default configurations: number of max. allowed assertions for battleground games
    public static final int DEFAULT_NB_ASSERTIONS = 2;

}
