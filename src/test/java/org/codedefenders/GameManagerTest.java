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
package org.codedefenders;

import org.codedefenders.game.GameClass;
import org.codedefenders.servlets.auth.LoginManager;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.codedefenders.util.Constants.MUTANTS_DIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author Jose Rojas
 */
public class GameManagerTest {

	String originalMutantsDir;

	@Before
	public void setUp() {
		originalMutantsDir = Constants.MUTANTS_DIR;
		Constants.MUTANTS_DIR = org.apache.commons.io.FileUtils.getTempDirectoryPath();
		File folder = new File(MUTANTS_DIR);
		folder.mkdir();
	}

	@After
	public void tearDown() {
		File folder = new File(MUTANTS_DIR);
		folder.delete();
		Constants.MUTANTS_DIR = originalMutantsDir;
	}

	@Test @Ignore
	public void testServlet() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);

		when(request.getParameter("username")).thenReturn("jose");
		when(request.getParameter("password")).thenReturn("jose");
		PrintWriter writer = new PrintWriter("somefile.txt");
		when(response.getWriter()).thenReturn(writer);

		new LoginManager().doPost(request, response);

		verify(request, atLeast(1)).getParameter("username"); // only if you want to verify username was called...
		writer.flush(); // it may not have been flushed yet...
		assertTrue(org.apache.commons.io.FileUtils.readFileToString(new File("somefile.txt"), "UTF-8")
				.contains("My Expected String"));
	}

	@Test
	public void testGetNextSubDirEmpty() throws IOException {
		File folder = getCleanTmpGameDir(1);
		assertEquals(folder.getAbsolutePath() + File.separator + "00000001", FileUtils.getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	@Test
	public void testGetNextSubDirNonEmpty() throws IOException {
		File folder = getCleanTmpGameDir(1);
		File subfolder = new File(folder.getAbsolutePath() + File.separator + "00000001");
		subfolder.delete();
		subfolder.mkdir();
		assertEquals(folder.getAbsolutePath() + File.separator + "00000002", FileUtils.getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	@Test
	public void testGetNextSubDirTwo() throws IOException {
		File folder = getCleanTmpGameDir(1);
		File subfolder = new File(folder.getAbsolutePath() + File.separator + "00000001");
		subfolder.delete();
		subfolder.mkdir();
		File subfolder2 = new File(folder.getAbsolutePath() + File.separator + "00000002");
		subfolder2.delete();
		subfolder2.mkdir();
		assertEquals(folder.getAbsolutePath() + File.separator + "00000003", FileUtils.getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	@Test
	public void testGetNextSubDirMore() throws IOException, ServletException {
		File folder = getCleanTmpGameDir(1);
		File subfolder = new File(folder.getAbsolutePath() + File.separator + "00000001");
		subfolder.delete();
		subfolder.mkdir();
		File subfolder2 = new File(folder.getAbsolutePath() + File.separator + "00000002");
		subfolder2.delete();
		subfolder2.mkdir();
		File subfolder3 = new File(folder.getAbsolutePath() + File.separator + "foo");
		subfolder3.delete();
		subfolder3.mkdir();
		assertEquals(folder.getAbsolutePath() + File.separator + "00000003", FileUtils.getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	private File getCleanTmpGameDir(int gameId) throws IOException {
		File folder = new File(org.apache.commons.io.FileUtils.getTempDirectory().getAbsolutePath() + File.separator + "testCodeDefenders" + File.separator + gameId);
		folder.delete();
		folder.mkdirs();
		org.apache.commons.io.FileUtils.cleanDirectory(folder);
		return folder;
	}

	@Test
	public void testSUTClass(){
		assertEquals("Foo", new GameClass("Foo", "Foo", "" , "").getBaseName());
		assertEquals("Foo", new GameClass("org.Foo", "Foo2", "" , "").getBaseName());
		assertEquals("Foo", new GameClass("org.foo.bar.algo.mas.Foo", "Foo3", "" , "").getBaseName());
		assertEquals("", new GameClass("Foo", "Foo4", "" , "").getPackage());
		assertEquals("org", new GameClass("org.Foo", "Foo5", "" , "").getPackage());
		assertEquals("org.foo.bar", new GameClass("org.foo.bar.Foo", "Foo6", "" , "").getPackage());
	}
}
