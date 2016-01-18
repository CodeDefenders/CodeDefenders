package org.codedefenders;

/**
 * @author Jose Rojas
 */
public class Constants {

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static String DATA_DIR    = FILE_SEPARATOR + "WEB-INF" + FILE_SEPARATOR + "data";
	public static String CUTS_DIR    = DATA_DIR + FILE_SEPARATOR + "sources";
	public static String MUTANTS_DIR = DATA_DIR + FILE_SEPARATOR + "mutants";
	public static String TESTS_DIR   = DATA_DIR + FILE_SEPARATOR + "tests";

	public static final String TEST_PREFIX = "Test";
	public static final String JAVA_SOURCE_EXT = ".java";
	public static final String JAVA_CLASS_EXT = ".class";


	public static final String LOGIN_VIEW_JSP           = "jsp" + FILE_SEPARATOR + "login_view.jsp";
	public static final String RESOLVE_EQUIVALENCE_JSP  = "jsp" + FILE_SEPARATOR + "resolve_equivalence.jsp";
	public static final String ATTACKER_VIEW_JSP        = "jsp" + FILE_SEPARATOR + "attacker_view.jsp";
	public static final String DEFENDER_VIEW_JSP        = "jsp" + FILE_SEPARATOR + "defender_view.jsp";
	public static final String SCORE_VIEW_JSP           = "jsp" + FILE_SEPARATOR + "score_view.jsp";

	public static final String WINNER_MESSAGE = "You won!";
	public static final String LOSER_MESSAGE = "You won!";
	public static final String DRAW_MESSAGE = "It was a draw!";
}
