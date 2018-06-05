package org.codedefenders.util;

import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Jose Rojas
 */
public class Constants {

	public static final String F_SEP = System.getProperty("file.separator");

	public static String DATA_DIR;

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
	public static String BLOCK_ATTACKER = "block.attacker";

	//public static String DATA_DIR    = F_SEP + "WEB-INF" + F_SEP + "data";
	public static String CUTS_DIR = DATA_DIR + F_SEP + "sources";
	public static String MUTANTS_DIR = DATA_DIR + F_SEP + "mutants";
	public static String TESTS_DIR = DATA_DIR + F_SEP + "tests";
	public static String AI_DIR = DATA_DIR + F_SEP + "ai";

	public static final String TEST_PREFIX = "Test";
	public static final String JAVA_SOURCE_EXT = ".java";
	public static final String JAVA_CLASS_EXT = ".class";
	public static final String TEST_INFO_EXT = ".xml";
	public static final String SUITE_EXT = "_ESTest";

	public static final String GRACE_PERIOD_MESSAGE = "Game is now in grace period.";

	public static final String LOGIN_VIEW_JSP = "jsp" + F_SEP + "login_view.jsp";
	public static final String RESOLVE_EQUIVALENCE_JSP = "jsp" + F_SEP + "resolve_equivalence.jsp";
	public static final String ATTACKER_VIEW_JSP = "jsp" + F_SEP + "attacker_view.jsp";
	public static final String DEFENDER_VIEW_JSP = "jsp" + F_SEP + "defender_view.jsp";
	public static final String SCORE_VIEW_JSP = "jsp" + F_SEP + "score_view.jsp";
	public static final String UTESTING_VIEW_JSP = "jsp" + F_SEP + "utesting_view.jsp";
	public static final String ADMIN_USER_JSP = "/jsp" + F_SEP + "admin_user_mgmt.jsp";
	public static final String ADMIN_GAMES_JSP = "/jsp" + F_SEP + "admin_create_games.jsp";
	public static final String ADMIN_SETTINGS_JSP = "/jsp" + F_SEP + "admin_system_settings.jsp";
	public static final String ADMIN_MONITOR_JSP = "/jsp" + F_SEP + "admin_monitor_games.jsp";

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

	public static final String MUTANT_VALIDATION_SUCCESS_MESSAGE = "Your mutant complies with our rules.";
	public static final String MUTANT_VALIDATION_LINES_MESSAGE = "Invalid mutant, sorry! Removing or adding lines is not allowed.";
	public static final String MUTANT_VALIDATION_MODIFIER_MESSAGE = "Invalid mutant, sorry! Changing modifiers such as 'static' or 'public' is not allowed.";
	public static final String MUTANT_VALIDATION_COMMENT_MESSAGE = "Invalid mutant, sorry! Adding or modifying comments is not allowed.";
	public static final String MUTANT_VALIDATION_LOGIC_MESSAGE = "Invalid mutant, sorry! Your mutant contains new logical operations";
	public static final String MUTANT_VALIDATION_OPERATORS_MESSAGE = "Invalid mutant, sorry! Your mutant contains prohibited operations such as bitshifts, ternary operators, added comments or multiple statments per line.";
	public static final String MUTANT_VALIDATION_CALLS_MESSAGE = "Your mutant contains calls to System.*, Random.* or new control structures.\n\nShame on you!";
	public static final String MUTANT_VALIDATION_IDENTICAL_MESSAGE = "Invalid mutant, sorry! Your mutant is identical to the CUT";

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
	// JSP
	public static final String SESSION_ATTRIBUTE_PREVIOUS_TEST = "previousTest";
	public static final String SESSION_ATTRIBUTE_PREVIOUS_MUTANT = "previousMutant";

	public static final String MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE = "Invalid mutant, sorry! Your mutant changes one or more method signatures or field names or import statements";

	public static final String ATTACKER_HAS_PENDING_DUELS = "Sorry, your mutant cannot be accepted because you have pending equivalence duels !\nNo worries your mutant would be there ready to be submitted once you solve all your equivalence duels.";
}
