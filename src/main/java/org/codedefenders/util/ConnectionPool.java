package org.codedefenders.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implements the <code>Singleton</code>-pattern and object pooling. Handles
 * connections to the database by queuing them. Also checks the database driver
 * upon initialization and tries to refresh connections once if they do not work
 * when returned.
 *
 * @author wendling
 */
public final class ConnectionPool {

	private static ConnectionPool connectionPool;
	private List<Connection> connections = new ArrayList<>();
	private Queue<Connection> availableConnections = new LinkedList<>();
	private Logger logger = LoggerFactory.getLogger(ConnectionPool.class.getName());

	/**
	 * Amount of time a thread waits to be notified of newly available connections.
	 */
	private static final long WAITING_TIME = 5000;

	/**
	 * Number of constantly open connections.
	 */
	public static final int NB_CONNECTIONS = 20;

	private ConnectionPool() {
		init();
	}

	/**
	 * Initializes the connections list and queue of available connections.
	 */
	private void init() {
		logger.info("Initializing ConnectionPool...");
		try {
			Connection conn = DatabaseConnection.getConnection();
			conn.close();
		} catch (NamingException | SQLException e) {
			logger.warn("JDBC Driver not found.", e);
			throw new StorageException();
		}

		Connection newConnection = null;
		try {
			for (int i = 0; i < NB_CONNECTIONS; ++i) {
				newConnection = refresh(newConnection);
				connections.add(newConnection);
				availableConnections.add(connections.get(i));
			}
		} catch (SQLException e) {
			logger.error("SQL exception while opening connections.", e);
			closeDBConnections();
			throw new StorageException();
		}
		logger.info("ConnectionPool initialized.");
	}

	/**
	 * Returns the singleton object.
	 *
	 * @return single <code> ConnectionPool</code> object.
	 */
	public static ConnectionPool getInstanceOf() {
		if (connectionPool == null) {
			connectionPool = new ConnectionPool();
		}
		return connectionPool;
	}

	/**
	 * Closes all data base connections in the queue.
	 */
	public void closeDBConnections() {
		logger.info("Closing ConnectionPool connections...");
		// Needed to avoid ConcurrentModificationException when iterating
		// and removing
		List<Connection> closedConnections = new ArrayList<Connection>();
		try {
			for (Connection connection : connections) {
				connection.close();
				if (availableConnections.contains(connection)) {
					availableConnections.remove(connection);
				}
				closedConnections.add(connection);
			}

		} catch (SQLException e) {
			logger.warn("SQL exception while closing connections.");
			throw new StorageException();
		} finally {
			connections.removeAll(closedConnections);
		}
		logger.info("Closed ConnectionPool connections successfully.");
	}

	/**
	 * Returns an unused connection from the queue. Tries to refresh connections
	 * once if they do not work.
	 *
	 * @return a free connection or null if none is available.
	 * @throws NoMoreConnectionsException if there are no free connections
	 */
	public synchronized Connection getDBConnection()
			throws NoMoreConnectionsException {
		Connection returnConn;
		if (availableConnections.peek() != null) {
			returnConn = availableConnections.poll();
		} else {
			try {
				wait(WAITING_TIME);
			} catch (InterruptedException e) {
				logger.warn("The DB thread was interrupted while waiting for a connection.");
				throw new Error();
			}
			if (availableConnections.peek() == null) {
				logger.warn("Threw NoMoreConnectionsException.");
				throw new NoMoreConnectionsException();
			}
			returnConn = availableConnections.poll();
		}
		try {
			returnConn.createStatement().execute("SELECT 1;");
		} catch (SQLException e) {
			logger.info("Refreshing SQL connection: " + returnConn + ".");
			try {
				returnConn = refresh(returnConn);
				if (returnConn.isClosed() || !returnConn.createStatement().execute("SELECT 1;")) {
					logger.error("Connection could not be refreshed.");
					throw new StorageException();
				}
				logger.info("SQL connection " + returnConn + " refreshed successfully.");
			} catch (SQLException e1) {
				logger.error("SQL exception while refreshing connection: " + returnConn + ".", e1);
				throw new StorageException();
			}

		}
		return returnConn;
	}

	private Connection refresh(Connection returnConn) throws SQLException {
		try {
			return DatabaseConnection.getConnection();
		} catch (NamingException e) {
			logger.warn("JDBC Driver not found.", e);
			throw new StorageException();
		}
	}

	/**
	 * Releases a previously gotten connection so it is usable for other objects
	 * again.
	 *
	 * @param connection to be released.
	 */
	public synchronized void releaseDBConnection(Connection connection) {
		if (connection != null) {
			availableConnections.add(connection);
			/* disabled as currently we are using autocommit
			try {
				connection.rollback();
			} catch (SQLException e) {
				logger.error("SQL exception while releasing connections and "
						+ "rolling back transactions.", e);
				throw new StorageException(e.getMessage());
			}*/
			notifyAll();
		}
	}

	/**
	 * Exception to be thrown by <code>ConnectionPool</code> and caught in calling
	 * objects if it has no more available connections upon request.
	 *
	 * @author wendling
	 */

	public class NoMoreConnectionsException extends Exception {

		private static final long serialVersionUID = 1L;

		NoMoreConnectionsException() {

		}

		public NoMoreConnectionsException(String message) {
			super(message);
		}
	}

	/**
	 * Unchecked Exception for cases in which the data base is not available (wrong
	 * driver, host,...) or corrupt (missing tables or entries) and functionality
	 * is impeded.
	 *
	 * @author wendling
	 */
	public class StorageException extends RuntimeException {

		private static final long serialVersionUID = -6071779646574124576L;

		StorageException() {

		}

		public StorageException(String message) {
			super(message);
		}
	}

}
