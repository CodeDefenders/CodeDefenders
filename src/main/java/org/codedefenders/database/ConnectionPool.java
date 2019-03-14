/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

/**
 * This class manages database connection by collecting connections in a pool which can be queried.
 * Queried connections have to be released to the pool again, otherwise the pool is leaking.
 *
 * Available connections are handled in a {@link Queue}.
 *
 * The connection pool limits the number of instances to one. The instance can
 * get retrieved using {@link #instance()}.
 */
public final class ConnectionPool {
    private Logger logger = LoggerFactory.getLogger(ConnectionPool.class.getName());

    private static ConnectionPool INSTANCE;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_TIMEOUT = 5000;

    private List<Connection> connections;
    private Queue<Connection> availableConnections;
    /**
     * Amount of time a thread waits to be notified of newly available connections.
     */
    private long waitingTime = TimeUnit.SECONDS.toMillis(5);

    /**
     * Number of constantly open connections.
     */
    private int nbConnections = 20;

    /**
     * @return the connection pool. There should always only be one instance.
     */
    public static synchronized ConnectionPool instance() {
        if (INSTANCE == null) {
            INSTANCE = new ConnectionPool();
        }
        return INSTANCE;
    }

    private ConnectionPool() {
        init();
    }

    /**
     * Initializes the connections list and queue of available connections.
     */
    private void init() {
        for (int i = 1; i <= MAX_RETRIES; i++) {
            logger.info("Initializing ConnectionPool... (retry " + i + " of " + MAX_RETRIES + ")");
            try {
                Connection conn = DatabaseConnection.getConnection();
                getParametersFromDB(conn);
                conn.close();
                // If we managed to connect, we can proceed. Otherwise, we retry
                break;
            } catch (NamingException | SQLException e) {
                logger.warn("Cannot connect to the Database", e);
                if (i == MAX_RETRIES) {
                    logger.warn("Give up and fail deployment");
                    throw new NoMoreConnectionsException("Could not initialize the database. Aborting.");
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

        Connection newConnection;
        availableConnections = new LinkedList<>();
        connections = new ArrayList<>();

        try {
            for (int i = 0; i < nbConnections; ++i) {
                newConnection = refreshConnection();
                connections.add(newConnection);
                availableConnections.add(connections.get(i));
            }
        } catch (SQLException e) {
            logger.error("SQL exception while opening connections.", e);
            closeDBConnections();
            throw new UncheckedSQLException(e);
        }
        logger.info("ConnectionPool initialized with " + nbConnections + " connections.");
    }

    /**
     * Closes all data base connections in the queue.
     */
    public void closeDBConnections() {
        logger.info("Closing ConnectionPool connections...");
        // Needed to avoid ConcurrentModificationException when iterating
        // and removing
        List<Connection> closedConnections = new ArrayList<>();
        try {
            for (Connection connection : connections) {
                connection.close();
                availableConnections.remove(connection);
                closedConnections.add(connection);
            }

        } catch (SQLException e) {
            logger.warn("SQL exception while closing connections.", e);
            // otherwise ignored
        } finally {
            connections.removeAll(closedConnections);
        }
        logger.info("Closed ConnectionPool connections successfully.");
        INSTANCE = null;
    }

    /**
     * Returns an unused connection from the queue. Tries to refreshConnection connections
     * once if they do not work.
     *
     * @return a free connection or null if none is available.
     * @throws NoMoreConnectionsException if there are no free connections
     */
    synchronized Connection getDBConnection() throws NoMoreConnectionsException {
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
                throw new NoMoreConnectionsException("No connections available.");
            }
            returnConn = availableConnections.poll();
        }
        try {
            returnConn.createStatement().execute("SELECT 1;");
        } catch (SQLException e) {
            logger.info("Refreshing SQL connection: " + returnConn + ".");
            try {
                returnConn = refreshConnection();
                if (returnConn.isClosed() || !returnConn.createStatement().execute("SELECT 1;")) {
                    logger.error("Connection could not be refreshed.");
                    throw new NoMoreConnectionsException("No connections available. Refreshed connection is closed or failed to execute statement");
                }
                logger.info("SQL connection {} refreshed successfully.", returnConn);
            } catch (SQLException e1) {
                logger.error("SQL exception while refreshing connection: " + returnConn + ".", e1);
                throw new NoMoreConnectionsException("No connections available.");
            }

        }
        return returnConn;
    }

    private Connection refreshConnection() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (NamingException e) {
            logger.warn("JDBC Driver not found.", e);
            throw new UncheckedSQLException("Could not update database connection.");
        }
    }

    /**
     * Releases a previously gotten connection so it is usable for other objects
     * again.
     *
     * @param connection to be released.
     */
    synchronized void releaseDBConnection(Connection connection) {
        if (connection != null) {
            availableConnections.add(connection);
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
                    logger.error("Failed to remove database connection from the pool.", e);
                }
            }
            nbConnections = newSize;
            connections = connections.subList(0, nbConnections);
        } else if (newSize > nbConnections) {
            for (int i = nbConnections; i < newSize; i++) {
                try {
                    Connection newConnection = refreshConnection();
                    connections.add(newConnection);
                    availableConnections.add(connections.get(i));
                } catch (SQLException e) {
                    logger.error("Failed to add database connection to the pool.", e);
                }
            }
            nbConnections = newSize;
        }
    }

    public void updateWaitingTime(int newWaitingTime) {
        waitingTime = newWaitingTime;
    }

    /**
     * This does not close the given {@link Connection}.
     */
    private void getParametersFromDB(Connection conn) throws SQLException {
        final AdminSystemSettings.SettingsDTO conns = AdminDAO.getSystemSettingInt(AdminSystemSettings.SETTING_NAME.CONNECTION_POOL_CONNECTIONS, conn);
        if (conns == null) {
            logger.warn("Could not retrieve CONNECTION_POOL_CONNECTIONS from database. Using default value.");
        } else {
            nbConnections = conns.getIntValue();
        }
        final AdminSystemSettings.SettingsDTO waitingTime = AdminDAO.getSystemSettingInt(AdminSystemSettings.SETTING_NAME.CONNECTION_WAITING_TIME, conn);
        if (waitingTime == null) {
            logger.warn("Could not retrieve CONNECTION_WAITING_TIME from database. Using default value.");
        } else {
            this.waitingTime = waitingTime.getIntValue();
        }
    }

    int getNbConnections() {
        return nbConnections;
    }

    /**
     * Exception to be thrown by <code>ConnectionPool</code> and caught in calling
     * objects if it has no more available connections upon request.
     *
     * @author wendling
     */
    class NoMoreConnectionsException extends RuntimeException {
        NoMoreConnectionsException(String message) {
            super(message);
        }
    }
}
