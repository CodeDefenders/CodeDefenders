/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.configuration.Configuration;

/**
 * This class contains most constants used in Code Defenders.
 * For URL paths, see {@link org.codedefenders.util.Paths}.
 *
 * @see org.codedefenders.util.Paths
 */
public class Constants {

    // TODO(Alex): Remove workaround when the *_CLASSPATH constants below are exposed in a better way
    public static final Configuration config = CDIUtil.getBeanFromCDI(Configuration.class);

    // Dummy game
    public static final int DUMMY_GAME_ID = -1;

    // Dummy user IDs
    public static final int DUMMY_CREATOR_USER_ID = -1;
    public static final int DUMMY_ATTACKER_USER_ID = 3;
    public static final int DUMMY_DEFENDER_USER_ID = 4;

    // dependencies, mutants and tests subdirectories for CUTs
    public static final String CUTS_DEPENDENCY_DIR = "dependencies";
    public static final String CUTS_MUTANTS_DIR = "mutants";
    public static final String CUTS_TESTS_DIR = "tests";

    // Puzzle and Battleground mutant and test folders
    public static final String MODE_PUZZLE_DIR = "puzzle";
    public static final String MODE_BATTLEGROUND_DIR = "mp";


    public static final String JACOCO_CLASSPATH = Stream.of(
                    "org.jacoco.core-0.8.8.jar",
                    "org.jacoco.report-0.8.8.jar",
                    "org.jacoco.agent-0.8.8.jar",
                    "org.jacoco.ant-0.8.8.jar"
            ).map(jarName -> config.getLibraryDir().toPath().resolve(jarName))
            .map(Path::toString)
            .collect(Collectors.joining(Character.toString(File.pathSeparatorChar)));

    public static final String COMMON_CLASSPATH = Stream.of(
                    // JUnit
                    "junit-4.13.2.jar",
                    "junit-jupiter-api-5.9.0.jar",
                    "opentest4j-1.2.0.jar",
                    "junit-platform-commons-1.9.0.jar",
                    // Mockito
                    "mockito-core-4.8.0.jar",
                    "objenesis-3.2.jar",
                    "byte-buddy-1.12.14.jar",
                    "byte-buddy-agent-1.12.14.jar",
                    // Hamcrest
                    "hamcrest-2.2.jar",
                    // Google Truth
                    "truth-1.1.3.jar",
                    "truth-java8-extension-1.1.3.jar",
                    "guava-31.1-jre.jar"
            ).map(jarName -> config.getLibraryDir().toPath().resolve(jarName))
            .map(Path::toString)
            .collect(Collectors.joining(Character.toString(File.pathSeparatorChar)));

    public static final String TEST_CLASSPATH = COMMON_CLASSPATH + File.pathSeparator + JACOCO_CLASSPATH;

    public static final String TEST_PREFIX = "Test";
    public static final String JAVA_SOURCE_EXT = ".java";
    public static final String JAVA_CLASS_EXT = ".class";
    public static final String TEST_INFO_EXT = ".xml";
    public static final String SUITE_EXT = "_ESTest";

    public static final String GRACE_PERIOD_MESSAGE = "Game is now in grace period.";

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

    public static final String BATTLEGROUND_GAME_VIEW_JSP = "/jsp/battleground/game_view.jsp";
    public static final String BATTLEGROUND_DETAILS_VIEW_JSP = "/jsp/battleground/details_view.jsp";
    public static final String MELEE_GAME_VIEW_JSP = "/jsp/melee/game_view.jsp";
    public static final String MELEE_DETAILS_VIEW_JSP = "/jsp/melee/details_view.jsp";

    public static final String PUZZLE_OVERVIEW_VIEW_JSP = "/jsp/puzzle/puzzle_overview.jsp";
    public static final String PUZZLE_GAME_ATTACKER_VIEW_JSP = "/jsp/puzzle/attacker_view.jsp";
    public static final String PUZZLE_GAME_DEFENDER_VIEW_JSP = "/jsp/puzzle/defender_view.jsp";

    public static final String USER_GAMES_OVERVIEW_JSP = "/jsp/user_games_view.jsp";
    public static final String GAMES_HISTORY_JSP = "/jsp/games_history.jsp";
    public static final String CLASS_UPLOAD_VIEW_JSP = "/jsp/upload_class_view.jsp";

    public static final String ADMIN_USER_JSP = "/jsp/admin_user_mgmt.jsp";
    public static final String ADMIN_GAMES_JSP = "/jsp/admin_create_games.jsp";
    public static final String ADMIN_CLASSES_JSP = "/jsp/admin_class_management.jsp";
    public static final String ADMIN_SETTINGS_JSP = "/jsp/admin_system_settings.jsp";
    public static final String ADMIN_MONITOR_JSP = "/jsp/admin_monitor_games.jsp";
    public static final String ADMIN_KILLMAPS_JSP = "/jsp/admin_killmap_management.jsp";
    public static final String ADMIN_PUZZLE_MANAGEMENT_JSP = "/jsp/admin_puzzle_management.jsp";
    public static final String ADMIN_PUZZLE_UPLOAD_JSP = "/jsp/admin_puzzle_upload.jsp";
    public static final String ADMIN_ANALYTICS_USERS_JSP = "/jsp/admin_analytics_users.jsp";
    public static final String ADMIN_ANALYTICS_CLASSES_JSP = "/jsp/admin_analytics_classes.jsp";
    public static final String ADMIN_ANALYTICS_KILLMAPS_JSP = "/jsp/admin_analytics_killmaps.jsp";

    // Messages
    public static final String WINNER_MESSAGE = "You won!";
    public static final String LOSER_MESSAGE = "You lost!";
    public static final String DRAW_MESSAGE = "It was a draw!";

    public static final String TEST_GENERIC_ERROR_MESSAGE
            = "Sorry! An error on the server prevented the compilation of your test.";

    public static final String TEST_DID_NOT_COMPILE_MESSAGE
            = "Your test did not compile. Try again, but with compilable code.";
    public static final String TEST_INVALID_MESSAGE = "Your test is not valid. Remember the rules: "
            + "Only one non-empty test, at most %d assertions per test, no conditionals and no loops!";
    public static final String TEST_PASSED_ON_CUT_MESSAGE
            = "Great! Your test compiled and passed on the original class under test.";
    public static final String TEST_DID_NOT_PASS_ON_CUT_MESSAGE
            = "Your test did not pass on the original class under test. Try again.";
    public static final String TEST_KILLED_CLAIMED_MUTANT_MESSAGE
            = "Yay, your test killed the allegedly equivalent mutant. You won the duel!";
    public static final String TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE
            = "Oh no, your test did not kill the possibly equivalent mutant! You lost the duel.";
    public static final String TEST_SUBMITTED_MESSAGE = "Test submitted and ready to kill mutants!";
    public static final String TEST_KILLED_ZERO_MESSAGE = "Your test has not killed any mutants, just yet.";
    public static final String TEST_KILLED_LAST_MESSAGE = "Great, your test killed the last mutant!";
    public static final String TEST_KILLED_ONE_MESSAGE = "Great, your test killed a mutant!";
    public static final String TEST_KILLED_N_MESSAGE = "Awesome! Your test killed %d mutants!"; // number of mutants

    public static final String MUTANT_COMPILED_MESSAGE = "Your mutant was compiled successfully.";
    public static final String MUTANT_ACCEPTED_EQUIVALENT_MESSAGE = "The mutant was accepted as equivalent.";
    public static final String MUTANT_UNCOMPILABLE_MESSAGE = "Your mutant failed to compile. Try again.";

    public static final String MUTANT_INVALID_MESSAGE = "Invalid mutant, sorry! Your mutant is identical to the CUT or "
            + "it contains invalid code (ifs, loops, or new logical ops.)";
    public static final String MUTANT_CREATION_ERROR_MESSAGE
            = "Oops! Something went wrong and the mutant was not created.";
    public static final String MUTANT_DUPLICATED_MESSAGE = "Sorry, your mutant already exists in this game!";
    public static final String MUTANT_CLAIMED_EQUIVALENT_MESSAGE
            = "Mutant claimed as equivalent, waiting for attacker to respond.";
    public static final String MUTANT_CLAIMED_EQUIVALENT_ERROR_MESSAGE
            = "Something went wrong claiming an equivalent mutant"; // TODO: How?
    public static final String MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE
            = "Cheeky! You cannot claim equivalence on untested lines!";
    public static final String MUTANT_KILLED_BY_TEST_MESSAGE
            = "Test %d killed your mutant. Better luck with the next one!"; // test
    public static final String MUTANT_SUBMITTED_MESSAGE = "Mutant submitted, may the force be with it.";
    public static final String MUTANT_ALIVE_1_MESSAGE = "Cool, your mutant survived its first test.";
    public static final String MUTANT_ALIVE_N_MESSAGE
            = "Awesome, your mutant survived %d tests!"; // number of tests that covered mutant

    // Request attributes
    public static final String REQUEST_ATTRIBUTE_PUZZLE_GAME = "active_user_puzzle_game";

    public static final String ATTACKER_HAS_PENDING_DUELS
            = "Sorry, your mutant cannot be accepted because you have pending equivalence duels!\n"
            + "No worries your mutant would be there ready to be submitted once you solve all your equivalence duels.";

    public static final String DEFAULT_KILL_MESSAGE = "Sorry, no kill message available for this mutant";
}
