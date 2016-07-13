package org.codedefenders;

/**
 * @author Jose Rojas
 */
public class Constants {

	public static final String F_SEP = System.getProperty("file.separator");

	public static String DATA_DIR = "C:\\work\\cdef";
	//public static String DATA_DIR    = F_SEP + "WEB-INF" + F_SEP + "data";
	public static String CUTS_DIR    = DATA_DIR + F_SEP + "sources";
	public static String MUTANTS_DIR = DATA_DIR + F_SEP + "mutants";
	public static String TESTS_DIR   = DATA_DIR + F_SEP + "tests";
	public static String AI_DIR      = DATA_DIR + F_SEP + "ai";

	public static final String TEST_PREFIX = "Test";
	public static final String JAVA_SOURCE_EXT = ".java";
	public static final String JAVA_CLASS_EXT = ".class";
	public static final String TEST_INFO_EXT = ".xml";

	public static final String LOGIN_VIEW_JSP           = "jsp" + F_SEP + "login_view.jsp";
	public static final String RESOLVE_EQUIVALENCE_JSP  = "jsp" + F_SEP + "resolve_equivalence.jsp";
	public static final String ATTACKER_VIEW_JSP        = "jsp" + F_SEP + "attacker_view.jsp";
	public static final String DEFENDER_VIEW_JSP        = "jsp" + F_SEP + "defender_view.jsp";
	public static final String SCORE_VIEW_JSP           = "jsp" + F_SEP + "score_view.jsp";
	public static final String UTESTING_VIEW_JSP        = "jsp" + F_SEP + "utesting_view.jsp";

	// Messages
	public static final String WINNER_MESSAGE = "You won!";
	public static final String LOSER_MESSAGE = "You lost!";
	public static final String DRAW_MESSAGE = "It was a draw!";

	public static final String TEST_DID_NOT_COMPILE_MESSAGE = "Your test did not compile. Try again, but with compilable code.";
	public static final String TEST_INVALID_MESSAGE = "Your test is not valid. Remember the rules: Only one non-empty test, no conditionals and no loops!";
	public static final String TEST_PASSED_ON_CUT_MESSAGE = "Great! Your test compiled and passed on the original class under test.";
	public static final String TEST_DID_NOT_PASS_ON_CUT_MESSAGE = "Your test did not pass on the original class under test. Try again.";
	public static final String TEST_KILLED_CLAIMED_MUTANT_MESSAGE = "Yay, your test killed the allegedly equivalent mutant! You won the duel.";
	public static final String TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE = "Oh no, your test did not kill the possibly equivalent mutant! You lost the duel.";
	public static final String TEST_SUBMITTED_MESSAGE = "Test submitted and ready to kill mutants!";
	public static final String TEST_KILLED_ZERO_MESSAGE = "Your test did not kill any mutant, just yet.";
	public static final String TEST_KILLED_LAST_MESSAGE = "Great, your test killed the last mutant!";
	public static final String TEST_KILLED_ONE_MESSAGE = "Great, your test killed a mutant!";
	public static final String TEST_KILLED_N_MESSAGE = "Awesome! Your test killed %d mutants!"; // number of mutants

	public static final String MUTANT_COMPILED_MESSAGE = "Your mutant was compiled successfully.";
	public static final String MUTANT_ACCEPTED_EQUIVALENT_MESSAGE = "The mutant was marked equivalent.";
	public static final String MUTANT_UNCOMPILABLE_MESSAGE = "Your mutant failed to compile. Try again.";
	public static final String MUTANT_IDENTICAL_MESSAGE = "Your mutant is not quite a mutant, it's identical to the class under test!";
	public static final String MUTANT_CLAIMED_EQUIVALENT_MESSAGE = "Mutant claimed as equivalent, waiting for attacker to respond.";
	public static final String MUTANT_CLAIMED_EQUIVALENT_ERROR_MESSAGE = "Something went wrong claiming equivalent mutant"; // TODO: How?
	public static final String MUTANT_KILLED_BY_TEST_MESSAGE = "Test %d killed your mutant. Better luck in your next turn!"; // test
	public static final String MUTANT_SUBMITTED_MESSAGE = "Mutant submitted, may the force be with it.";
	public static final String MUTANT_ALIVE_1_MESSAGE = "Cool, your mutant is alive.";
	public static final String MUTANT_ALIVE_N_MESSAGE = "Awesome, your mutant survived %d existing tests!"; // number of tests
	// JSP
	public static final String SESSION_ATTRIBUTE_PREVIOUS_TEST = "previousTest";
	public static final String SESSION_ATTRIBUTE_PREVIOUS_MUTANT = "previousMutant";
}
