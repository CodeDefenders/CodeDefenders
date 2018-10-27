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
package org.codedefenders.servlets;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Test;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.codedefenders.util.Constants.DATA_DIR;
import static org.codedefenders.util.Constants.F_SEP;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST;
import static org.codedefenders.util.Constants.TESTS_DIR;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_INVALID_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;
import static org.codedefenders.validation.CodeValidator.DEFAULT_NB_ASSERTIONS;

public class UnitTesting extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(UnitTesting.class);

	// Based on info provided, navigate to the correct view for the user
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Get the session information specific to the current user.
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");
		Object ogid = session.getAttribute("gid");
		DuelGame activeGame;
		if (ogid == null) {
			System.out.println("Getting active unit testing session for user " + uid);
			activeGame = DatabaseAccess.getActiveUnitTestingSession(uid);
		} else {
			int gid = (Integer) ogid;
			System.out.println("Getting game " + gid + " for " + uid);
			activeGame = DatabaseAccess.getGameForKey("ID", gid);
		}
		session.setAttribute("game", activeGame);

		RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.UTESTING_VIEW_JSP);
		dispatcher.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("Executing doPost");

		ArrayList<String> messages = new ArrayList<>();
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");
		DuelGame activeGame = (DuelGame) session.getAttribute("game");
		session.setAttribute("messages", messages);

		// Get the text submitted by the user.
		String testText = request.getParameter("test");

		// If it can be written to file and compiled, end turn. Otherwise, dont.
		Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "sp");
		if (newTest == null) {
			messages.add(String.format(TEST_INVALID_MESSAGE, DEFAULT_NB_ASSERTIONS));
			session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
			response.sendRedirect(request.getContextPath()+"/utesting");
			return;
		}
		System.out.println("New Test " + newTest.getId());
		TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

		if (compileTestTarget.status.equals("SUCCESS")) {
			TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
			if (testOriginalTarget.status.equals("SUCCESS")) {
				messages.add(TEST_PASSED_ON_CUT_MESSAGE);
				activeGame.endRound();
				activeGame.update();
				if (activeGame.getState().equals(GameState.FINISHED))
					messages.add("Great! Unit testing goal achieved. Session finished.");
			} else {
				// testOriginalTarget.state.equals("FAIL") || testOriginalTarget.state.equals("ERROR")
				messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
				messages.add(testOriginalTarget.message);
				session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
			}
		} else {
			messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
			messages.add(compileTestTarget.message);
			session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
		}
		response.sendRedirect(request.getContextPath()+"/utesting");
	}

	/**
	 *
	 * @param gid
	 * @param cid
	 * @param testText
	 * @param ownerId
	 * @param subDirectory - Directy inside data to store information
	 * @return {@code null} if test is not valid
	 * @throws IOException
	 */
	public Test createTest(int gid, int cid, String testText, int ownerId, String subDirectory) throws IOException {

		GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

		File newTestDir = FileUtils.getNextSubDir(getServletContext().getRealPath(DATA_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + TESTS_DIR + F_SEP + ownerId));

		String javaFile = FileUtils.createJavaFile(newTestDir, classUnderTest.getBaseName(), testText);

		if (! validTestCode(javaFile)) {
			return null;
		}

		// Check the test actually passes when applied to the original code.
		Test newTest = AntRunner.compileTest(newTestDir, javaFile, gid, classUnderTest, ownerId);
		TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

		if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
			AntRunner.testOriginal(newTestDir, newTest);
		}
		return newTest;
	}

	private boolean validTestCode(String javaFile) throws IOException {

		CompilationUnit cu;
		FileInputStream in = null;
		try {
			in = new FileInputStream(javaFile);
			// parse the file
			cu = JavaParser.parse(in);
			// prints the resulting compilation unit to default system output
			if (cu.getTypes().size() != 1) {
				System.out.println("Invalid test suite contains more than one type declaration.");
				return false;
			}
			TypeDeclaration clazz = cu.getTypes().get(0);
			if (clazz.getMembers().size() != 1) {
				System.out.println("Invalid test suite contains more than one method.");
				return false;
			}
			MethodDeclaration test = (MethodDeclaration)clazz.getMembers().get(0);
			BlockStmt testBody = test.getBody();
			for (Node node : testBody.getChildrenNodes()) {
				if (node instanceof ForeachStmt
						|| node instanceof IfStmt
						|| node instanceof ForStmt
						|| node instanceof WhileStmt
						|| node instanceof DoStmt) {
					System.out.println("Invalid test contains " + node.getClass().getSimpleName() + " statement");
					return false;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			in.close();
		}
		return true;
	}
}
