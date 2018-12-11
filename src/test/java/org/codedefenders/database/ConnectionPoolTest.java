package org.codedefenders.database;

import org.codedefenders.itests.DatabaseTest;
import org.codedefenders.rules.DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the {@link ConnectionPool} implementation. Extracted from {@link DatabaseTest}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseConnection.class})
public class ConnectionPoolTest {

    @Rule
    public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

    @Before
    public void mockDBConnections() throws Exception {
        PowerMockito.mockStatic(DatabaseConnection.class);
        PowerMockito.when(DatabaseConnection.getConnection()).thenAnswer(new Answer<Connection>() {
            public Connection answer(InvocationOnMock invocation) throws SQLException {
                // Return a new connection from the rule instead
                return db.getConnection();
            }
        });
    }

    @Test
    public void testConnectionPoolRelease() throws ConnectionPool.NoMoreConnectionsException {
        final ConnectionPool connectionPool = ConnectionPool.getInstanceOf();

        final int nbConnectionsBefore = connectionPool.getNbConnections();
        assertTrue(nbConnectionsBefore > 0);

        final Connection connection = connectionPool.getDBConnection();
        connectionPool.releaseDBConnection(connection);

        final int nbConnectionsAfter = connectionPool.getNbConnections();

        assertEquals(nbConnectionsBefore, nbConnectionsAfter);
    }

    /**
     * Requests one more connection than the pool can give and checks if the connection pool fails.
     *
     * All connections are released after.
     */
    @Test
    public void testConnectionPoolLimit() throws ConnectionPool.NoMoreConnectionsException {
        final ConnectionPool connectionPool = ConnectionPool.getInstanceOf();
        int numberOfConnections = connectionPool.getNbConnections();

        final List<Connection> stuckConnections = new ArrayList<>(numberOfConnections);

        for (int i = 0; i < numberOfConnections; ++i) {
            stuckConnections.add(connectionPool.getDBConnection());
        }

        try {
            connectionPool.getDBConnection();
            fail("ConnectionPool should be empty.");
        } catch (ConnectionPool.NoMoreConnectionsException ignored) {
            // expected behavior
        }

        for (Connection stuckConnection : stuckConnections) {
            connectionPool.releaseDBConnection(stuckConnection);
        }

        assertEquals(numberOfConnections, connectionPool.getNbConnections());
    }
}
