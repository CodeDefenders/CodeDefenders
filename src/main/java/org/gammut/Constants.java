package org.gammut;

/**
 * @author Jose Rojas
 */
public class Constants {

	public static String MUTANTS_DIR = "/WEB-INF/data/mutants";
	public static String TESTS_DIR = "/WEB-INF/data/tests";

	public static final String TEST_PREFIX = "Test";
	public static final String JAVA_SOURCE_EXT = ".java";
	public static final String JAVA_CLASS_EXT = ".class";
	public static final String SEPARATOR = "/";

	public static final String LOGIN_VIEW_JSP = "jsp/login_view.jsp";
	public static final String RESOLVE_EQUIVALENCE_JSP = "jsp/resolve_equivalence.jsp";
	public static final String ATTACKER_VIEW_JSP = "jsp/attacker_view.jsp";
	public static final String DEFENDER_VIEW_JSP = "jsp/defender_view.jsp";
	public static final String SCORE_VIEW_JSP = "jsp/score_view.jsp";


	/* Mutant Equivalence */
	public enum Equivalence { ASSUMED_NO, PENDING_TEST, DECLARED_YES, ASSUMED_YES, PROVEN_NO};

}
