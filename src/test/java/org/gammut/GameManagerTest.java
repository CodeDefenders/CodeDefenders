package org.gammut;

import static org.gammut.Constants.MUTANTS_DIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * @author Jose Rojas
 */
public class GameManagerTest {

	String originalMutantsDir;

	@Before
	public void setUp() {
		originalMutantsDir = Constants.MUTANTS_DIR;
		Constants.MUTANTS_DIR = FileUtils.getTempDirectoryPath();
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
		assertTrue(FileUtils.readFileToString(new File("somefile.txt"), "UTF-8")
				.contains("My Expected String"));
	}


	@Test @Ignore
	public void testCreateMutant() throws IOException {

		GameManager gm = new GameManager();
		gm.createTest(999, 999, "public class Foo { public int zero() { return 0; } }");
		gm.createMutant(999, 999, "public class Foo { public int zero() { return 1; } }");

	}

	@Test
	public void testGetNextSubDirEmpty() throws IOException {
		File folder = getCleanTmpGameDir(1);
		assertEquals(folder.getAbsolutePath() + "/1", new GameManager().getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	@Test
	public void testGetNextSubDirNonEmpty() throws IOException {
		File folder = getCleanTmpGameDir(1);
		File subfolder = new File(folder.getAbsolutePath() + "/1");
		subfolder.delete();
		subfolder.mkdir();
		assertEquals(folder.getAbsolutePath() + "/2", new GameManager().getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	@Test
	public void testGetNextSubDirTwo() throws IOException {
		File folder = getCleanTmpGameDir(1);
		File subfolder = new File(folder.getAbsolutePath() + "/1");
		subfolder.delete();
		subfolder.mkdir();
		File subfolder2 = new File(folder.getAbsolutePath() + "/10");
		subfolder2.delete();
		subfolder2.mkdir();
		assertEquals(folder.getAbsolutePath() + "/11", new GameManager().getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	@Test
	public void testGetNextSubDirMore() throws IOException, ServletException {
		File folder = getCleanTmpGameDir(1);
		File subfolder = new File(folder.getAbsolutePath() + "/1");
		subfolder.delete();
		subfolder.mkdir();
		File subfolder2 = new File(folder.getAbsolutePath() + "/10");
		subfolder2.delete();
		subfolder2.mkdir();
		File subfolder3 = new File(folder.getAbsolutePath() + "/foo11");
		subfolder3.delete();
		subfolder3.mkdir();
		assertEquals(folder.getAbsolutePath() + "/11", new GameManager().getNextSubDir(folder.getAbsolutePath()).getAbsolutePath());
	}

	private File getCleanTmpGameDir(int gameId) throws IOException {
		File folder = new File(FileUtils.getTempDirectory().getAbsolutePath() + "/testGamMut/" + gameId);
		folder.delete();
		folder.mkdirs();
		FileUtils.cleanDirectory(folder);
		return folder;
	}

}
