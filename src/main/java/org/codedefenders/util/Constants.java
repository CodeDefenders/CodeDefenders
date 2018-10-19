/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Jose Rojas
 */
public class Constants {

	public static final String F_SEP = System.getProperty("file.separator");

	public static final String DATA_DIR;

	static {
		// First check the Web abb context
		InitialContext initialContext;
		String dataHome = null;
		try {
			initialContext = new InitialContext();
			Context environmentContext = (Context) initialContext.lookup("java:/comp/env");
			dataHome = (String) environmentContext.lookup("data.dir");

		} catch (NamingException e) {
			// e.printStackTrace();
		}

		// Check Env
		if (dataHome == null) {
			ProcessBuilder pb = new ProcessBuilder();
			Map env = pb.environment();
			dataHome = (String) env.get("CODEDEFENDERS_DATA");
		}
		// Check System properties
		if (dataHome == null) {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				dataHome = System.getProperty("codedefenders.data", "C:/codedefenders-data");
			} else {
				dataHome = System.getProperty("codedefenders.data", "/var/lib/codedefenders");
			}
		}

		DATA_DIR = dataHome;
	}

	// Configuration variable names
	public static final String BLOCK_ATTACKER = "block.attacker";

	// Dummy game
	public static final int DUMMY_GAME_ID = -1;

	// Dummy user IDs
	public static final int DUMMY_CREATOR_USER_ID = -1;
	public static final int DUMMY_ATTACKER_USER_ID = 3;
	public static final int DUMMY_DEFENDER_USER_ID = 4;

	//public static String DATA_DIR    = F_SEP + "WEB-INF" + F_SEP + "data";
	public static final String CUTS_DIR = DATA_DIR + F_SEP + "sources";
	// dependencies, mutants and tests subdirectories for CUTs
	public static final String CUTS_DEPENDENCY_DIR = "dependencies";
	public static final String CUTS_MUTANTS_DIR = "mutants";
	public static final String CUTS_TESTS_DIR = "tests";

	// Puzzle, Duel and Battleground mutant and test folders
	public static final String MODE_PUZZLE_DIR = "puzzle";
	public static final String MODE_DUEL_DIR = "sp";
	public static final String MODE_BATTLEGROUND_DIR = "mp";

	// FIXME Phil: MUTANTS_DIR should be final.
	public static String MUTANTS_DIR = DATA_DIR + F_SEP + "mutants";
	public static final String TESTS_DIR = DATA_DIR + F_SEP + "tests";
	public static final String AI_DIR = DATA_DIR + F_SEP + "ai";

	public static final String LIB_JUNIT = DATA_DIR + F_SEP + "lib" + F_SEP + "junit-4.12.jar";
	public static final String LIB_HAMCREST = DATA_DIR + F_SEP + "lib" + F_SEP + "hamcrest-all-1.3.jar";
	public static final String LIB_MOCKITO = DATA_DIR + F_SEP + "lib" + F_SEP + "mockito-all-1.9.5.jar";

	public static final String TEST_CLASSPATH = Constants.LIB_JUNIT + File.pathSeparatorChar + Constants.LIB_HAMCREST + File.pathSeparatorChar + Constants.LIB_MOCKITO;
    public static final String TEST_CLASSPATH_WITH_DIR = TEST_CLASSPATH + File.pathSeparatorChar + "%s";
	public static final String TEST_CLASSPATH_WITH_2DIR = TEST_CLASSPATH_WITH_DIR + File.pathSeparatorChar + "%s";


	public static final String TEST_PREFIX = "Test";
	public static final String JAVA_SOURCE_EXT = ".java";
	public static final String JAVA_CLASS_EXT = ".class";
	public static final String TEST_INFO_EXT = ".xml";
	public static final String SUITE_EXT = "_ESTest";

	public static final String GRACE_PERIOD_MESSAGE = "Game is now in grace period.";

	public static final String LOGIN_VIEW_JSP = "jsp" + F_SEP + "login_view.jsp";
	public static final String SCORE_VIEW_JSP = "jsp" + F_SEP + "score_view.jsp";
	public static final String UTESTING_VIEW_JSP = "jsp" + F_SEP + "utesting_view.jsp";

	public static final String DUEL_RESOLVE_EQUIVALENCE_JSP = "jsp/duel/equivalence_view.jsp";
	public static final String DUEL_ATTACKER_VIEW_JSP = "jsp/duel/attacker_view.jsp";
	public static final String DUEL_DEFENDER_VIEW_JSP = "jsp/duel/defender_view.jsp";

	public static final String PUZZLE_OVERVIEW_PATH = "/puzzles";
	public static final String PUZZLE_GAME_PATH = "/puzzlegame";
	public static final String PUZZLE_GAME_SELECTION_PATH = "/puzzle/games";

	public static final String ADMIN_USER_JSP = "/jsp" + F_SEP + "admin_user_mgmt.jsp";
	public static final String ADMIN_GAMES_JSP = "/jsp" + F_SEP + "admin_create_games.jsp";
	public static final String ADMIN_SETTINGS_JSP = "/jsp" + F_SEP + "admin_system_settings.jsp";
	public static final String ADMIN_MONITOR_JSP = "/jsp" + F_SEP + "admin_monitor_games.jsp";
	public static final String ADMIN_ANALYTICS_USERS_JSP = "/jsp" + F_SEP + "admin_analytics_users.jsp";
	public static final String ADMIN_ANALYTICS_CLASSES_JSP = "/jsp" + F_SEP + "admin_analytics_classes.jsp";
	public static final String ADMIN_ANALYTICS_KILLMAP_JSP = "/jsp" + F_SEP + "admin_analytics_killmaps.jsp";

	public static final String NOTIFICATIONS = "/notifications";

	public static final String ADMIN_ANALYTICS_USERS = "/admin/analytics/users";
	public static final String ADMIN_ANALYTICS_CLASSES = "/admin/analytics/classes";
	public static final String ADMIN_ANALYTICS_KILLMAPS = "/admin/analytics/killmaps";

	public static final String API_ANALYTICS_USERS = "/api/users";
	public static final String API_ANALYTICS_CLASSES = "/api/classes";

	// Messages
	public static final String WINNER_MESSAGE = "You won!";
	public static final String LOSER_MESSAGE = "You lost!";
	public static final String DRAW_MESSAGE = "It was a draw!";

	public static final String TEST_GENERIC_ERROR_MESSAGE = "Sorry! An error on the server prevented the compilation of your test.";

	public static final String TEST_DID_NOT_COMPILE_MESSAGE = "Your test did not compile. Try again, but with compilable code.";
	public static final String TEST_INVALID_MESSAGE = "Your test is not valid. Remember the rules: Only one non-empty test, at most %d assertions per test, no conditionals and no loops!";
	public static final String TEST_PASSED_ON_CUT_MESSAGE = "Great! Your test compiled and passed on the original class under test.";
	public static final String TEST_DID_NOT_PASS_ON_CUT_MESSAGE = "Your test did not pass on the original class under test. Try again.";
	public static final String TEST_KILLED_CLAIMED_MUTANT_MESSAGE = "Yay, your test killed the allegedly equivalent mutant. You won the duel!";
	public static final String TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE = "Oh no, your test did not kill the possibly equivalent mutant! You lost the duel.";
	public static final String TEST_SUBMITTED_MESSAGE = "Test submitted and ready to kill mutants!";
	public static final String TEST_KILLED_ZERO_MESSAGE = "Your test did not kill any mutant, just yet.";
	public static final String TEST_KILLED_LAST_MESSAGE = "Great, your test killed the last mutant!";
	public static final String TEST_KILLED_ONE_MESSAGE = "Great, your test killed a mutant!";
	public static final String TEST_KILLED_N_MESSAGE = "Awesome! Your test killed %d mutants!"; // number of mutants

	public static final String MUTANT_COMPILED_MESSAGE = "Your mutant was compiled successfully.";
	public static final String MUTANT_ACCEPTED_EQUIVALENT_MESSAGE = "The mutant was accepted as equivalent.";
	public static final String MUTANT_UNCOMPILABLE_MESSAGE = "Your mutant failed to compile. Try again.";

	public static final String MUTANT_INVALID_MESSAGE = "Invalid mutant, sorry! Your mutant is identical to the CUT or it contains invalid code (ifs, loops, or new logical ops.)";
	public static final String MUTANT_CREATION_ERROR_MESSAGE = "Oops! Something went wrong and the mutant was not created.";
	public static final String MUTANT_DUPLICATED_MESSAGE = "Sorry, your mutant already exists in this game!";
	public static final String MUTANT_CLAIMED_EQUIVALENT_MESSAGE = "Mutant claimed as equivalent, waiting for attacker to respond.";
	public static final String MUTANT_CLAIMED_EQUIVALENT_ERROR_MESSAGE = "Something went wrong claiming equivalent mutant"; // TODO: How?
	public static final String MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE = "Cheeky! You cannot claim equivalence on untested lines!";
	public static final String MUTANT_KILLED_BY_TEST_MESSAGE = "Test %d killed your mutant. Better luck with the next one!"; // test
	public static final String MUTANT_SUBMITTED_MESSAGE = "Mutant submitted, may the force be with it.";
	public static final String MUTANT_ALIVE_1_MESSAGE = "Cool, your mutant survived its first test.";
	public static final String MUTANT_ALIVE_N_MESSAGE = "Awesome, your mutant survived %d tests!"; // number of tests that covered mutant

	public static final String SESSION_ATTRIBUTE_PREVIOUS_TEST = "previousTest";
	public static final String SESSION_ATTRIBUTE_PREVIOUS_MUTANT = "previousMutant";

	// Request attributes
	public static final String REQUEST_ATTRIBUTE_PUZZLE_GAME = "active_user_puzzle_game";

	public static final String ATTACKER_HAS_PENDING_DUELS = "Sorry, your mutant cannot be accepted because you have pending equivalence duels !\nNo worries your mutant would be there ready to be submitted once you solve all your equivalence duels.";
}
