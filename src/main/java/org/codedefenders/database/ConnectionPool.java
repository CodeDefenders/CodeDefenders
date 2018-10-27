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
package org.codedefenders.database;

import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.naming.NamingException;

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
	private List<Connection> connections;
	private Queue<Connection> availableConnections;
	private Logger logger = LoggerFactory.getLogger(ConnectionPool.class.getName());

	private static int MAX_RETRIES = 5;
	private static long RETRY_TIMEOUT = 5000;

	/**
	 * Amount of time a thread waits to be notified of newly available connections.
	 */
	private int waitingTime;

	/**
	 * Number of constantly open connections.
	 */
	private int nbConnections;

	private ConnectionPool() {
		init();
	}

	/**
	 * Initializes the connections list and queue of available connections.
	 */
	private void init() {
		for (int i = 1; i <= MAX_RETRIES; i++) {
			logger.info("Initializing ConnectionPool... (retry " + i + " of "+ MAX_RETRIES + ")");
			try {
				Connection conn = DatabaseConnection.getConnection();
				getParametersFromDB(conn);
				conn.close();
				// If we managed to connect, we can proceed. Otherwise, we retry
				break;
			} catch (StorageException | NamingException | SQLException e) {
				logger.warn("Cannot connect to the Database", e);
				if (i == MAX_RETRIES) {
					logger.warn("Give up and fail deployment");
					throw new StorageException("Could not initialize the database. Aborting.");
				} else {
					try {
						logger.info("Retry connecting to DB in " + RETRY_TIMEOUT + " msec ");
						Thread.sleep(RETRY_TIMEOUT);
					} catch (InterruptedException e1) {
						// Ignored
					}
				}
			}
		}

		Connection newConnection = null;
		availableConnections = new LinkedList<>();
		connections = new ArrayList<>();

		try {
			for (int i = 0; i < nbConnections; ++i) {
				newConnection = refresh(newConnection);
				connections.add(newConnection);
				availableConnections.add(connections.get(i));
			}
		} catch (SQLException e) {
			logger.error("SQL exception while opening connections.", e);
			closeDBConnections();
			throw new StorageException();
		}
		logger.info("ConnectionPool initialized with " + nbConnections + " connections.");
	}

	/**
	 * Returns the singleton object.
	 *
	 * @return single <code> ConnectionPool</code> object.
	 */
	public static synchronized ConnectionPool getInstanceOf() {
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
				availableConnections.remove(connection);
				closedConnections.add(connection);
			}

		} catch (SQLException e) {
			logger.warn("SQL exception while closing connections.");
			throw new StorageException();
		} finally {
			connections.removeAll(closedConnections);
		}
		logger.info("Closed ConnectionPool connections successfully.");
		connectionPool = null;
	}

	/**
	 * Returns an unused connection from the queue. Tries to refresh connections
	 * once if they do not work.
	 *
	 * @return a free connection or null if none is available.
	 * @throws NoMoreConnectionsException if there are no free connections
	 */
	public synchronized Connection getDBConnection() throws NoMoreConnectionsException {
		Connection returnConn;
		if (availableConnections.peek() != null) {
			returnConn = availableConnections.poll();
		} else {
			try {
				wait(waitingTime);
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

	public void updateSize(int newSize) {
		if (newSize < nbConnections) {
			for (int i = newSize; i < nbConnections; i++) {
				Connection conn = connections.get(i);
                availableConnections.remove(conn);
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			nbConnections = newSize;
			connections = connections.subList(0, nbConnections);
		} else if (newSize > nbConnections) {
			Connection newConnection = null;
			for (int i = nbConnections; i < newSize; i++) {
				try {
					newConnection = refresh(newConnection);
					connections.add(newConnection);
					availableConnections.add(connections.get(i));
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			nbConnections = newSize;
		}
	}

	public void updateWaitingTime(int newWaitingTime) {
		waitingTime = newWaitingTime;
	}

	private void getParametersFromDB(Connection conn) throws StorageException, SQLException {
		final AdminSystemSettings.SettingsDTO conns = AdminDAO.getSystemSettingInt(AdminSystemSettings.SETTING_NAME.CONNECTION_POOL_CONNECTIONS, conn);
		if (conns == null) {
			throw new StorageException("Could not retrieve CONNECTION_POOL_CONNECTIONS from database");
		}
		nbConnections = conns.getIntValue();
		final AdminSystemSettings.SettingsDTO waitingTime = AdminDAO.getSystemSettingInt(AdminSystemSettings.SETTING_NAME.CONNECTION_WAITING_TIME, conn);
		if (waitingTime == null) {
			throw new StorageException("Could not retrieve CONNECTION_WAITING_TIME from database");
		}
		this.waitingTime = waitingTime.getIntValue();
	}

	public int getNbConnections() {
		return nbConnections;
	}

	/**
	 * Exception to be thrown by <code>ConnectionPool</code> and caught in calling
	 * objects if it has no more available connections upon request.
	 *
	 * @author wendling
	 */
	public class NoMoreConnectionsException extends Exception {
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
	public static class StorageException extends RuntimeException {

		private static final long serialVersionUID = -6071779646574124576L;

		StorageException() {
		}

		public StorageException(String message) {
			super(message);
		}
	}
}
