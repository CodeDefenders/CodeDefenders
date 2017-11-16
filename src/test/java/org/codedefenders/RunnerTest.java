package org.codedefenders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.codedefenders.itests.IntegrationTest;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Jose Rojas
 */
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseConnection.class)
public class RunnerTest {

	@Rule
	DatabaseRule db = new DatabaseRule();

	@Test
	public void testInsertClasses() throws Exception {
		PowerMockito.mockStatic(DatabaseConnection.class);
		PowerMockito.when(DatabaseConnection.getConnection())
				.thenAnswer(new Answer<Connection>() {
					public Connection answer(InvocationOnMock invocation)
							throws SQLException {
						return DriverManager.getConnection(
								db.config.getURL(db.DBNAME), "root", "");
					}
				});
		assertEquals(0, DatabaseAccess.getAllClasses().size());

		GameClass cut = new GameClass("MyClass", "", "", "");
		assertTrue("Should have inserted class", cut.insert());
		assertEquals(1, DatabaseAccess.getAllClasses().size());

		cut = new GameClass("", "AliasForClass2", "", "");
		assertTrue("Should have inserted class", cut.insert());
		assertEquals(2, DatabaseAccess.getAllClasses().size());
		PowerMockito.verifyStatic();
	}
}
