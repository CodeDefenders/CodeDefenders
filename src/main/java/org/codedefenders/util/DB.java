package org.codedefenders.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DB {

    private static final Logger logger = LoggerFactory.getLogger(DB.class);

    public synchronized static Connection getConnection() {
        try {
            return DatabaseConnection.getConnection();
        } catch (Exception e) {
            logger.error("Database not available", e);
        }
        return null;
    }

    public static void cleanup(Connection c, Statement s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (SQLException se2) {
            logger.error("SQL exception caught", se2);
        }
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException se3) {
            logger.error("SQL exception caught", se3);
        }
    }

    public static PreparedStatement createPreparedStatement(Connection conn, String query, DatabaseValue value) {
        DatabaseValue[] databaseValues = {value};
        return createPreparedStatement(conn, query, databaseValues);
    }

    public static PreparedStatement createPreparedStatement(Connection conn, String query, DatabaseValue[] values) {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int count = 1;
            for (DatabaseValue val : values) {
                switch (val.getType()) {
                    case INT:
                        stmt.setInt(count++, val.getIntVal());
                        break;
                    case STRING:
                        stmt.setString(count++, val.getStringVal());
                        break;
                    case LONG:
                        stmt.setLong(count++, val.getLongVal());
                        break;
                    case FLOAT:
                        stmt.setFloat(count++, val.getFloatVal());
                        break;
                    case TIMESTAMP:
                        stmt.setTimestamp(count++, val.getTimestampVal());
                        break;
                    default:
                        return null;
                }
            }
        } catch (SQLException se) {
            logger.error("SQLException while creating Prepared Statement for query\n\t" + query, se);
            DB.cleanup(conn, stmt);
        }
        return stmt;
    }

    static PreparedStatement createPreparedStatement(Connection conn, String query) {
        PreparedStatement stmt = null;
        try {
            conn = DB.getConnection();
            stmt = conn.prepareStatement(query);
        } catch (SQLException se) {
            logger.error("SQLException while creating Statement for query\n\t" + query, se);
            DB.cleanup(conn, stmt);
        }
        return stmt;
    }


    /*
    * Does not clean up!
     */
    static ResultSet executeQueryReturnRS(Connection conn, PreparedStatement stmt) {
        try {
            stmt.executeQuery();
            return stmt.getResultSet();
        } catch (SQLException se) {
            logger.error("SQLException while getting Result Set for Statement\n\t" + stmt, se);
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static boolean executeUpdate(PreparedStatement stmt, Connection conn) {
        try {
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing Update for statement\n\t" + stmt, e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return false;
    }

    public static ResultSet executeQuery(PreparedStatement stmt, Connection conn) {
        try {
            return stmt.executeQuery();
        } catch (SQLException e) {
            logger.error("SQLException while executing Query for statement\n\t" + stmt, e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static int executeUpdateGetKeys(PreparedStatement stmt, Connection conn) {
        try {
            if (stmt.executeUpdate() > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing Update and getting generated Keys for statement\n\t" + stmt, e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return -1;
    }

    public static DatabaseValue getDBV(String v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(int v) {
        return new DatabaseValue(v);
    }

    /*
    * Caution: Explicitly cast to Long or value will be converted to float
    */
    public static DatabaseValue getDBV(float v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(Timestamp v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(Long v) {
        return new DatabaseValue(v);
    }

}
