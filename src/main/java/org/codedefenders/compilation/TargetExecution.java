package org.codedefenders.compilation;

import org.codedefenders.database.DB;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TargetExecution {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TargetExecution.class);
	public int id;
	public int testId;
	public int mutantId;
	public Target target;
	public String status;
	public String message;
	public Timestamp timestamp;

	public enum Target {COMPILE_MUTANT, COMPILE_TEST, TEST_ORIGINAL, TEST_MUTANT, TEST_EQUIVALENCE}

	// Constructors for initial creation of TargetExecution
	public TargetExecution(int tid, int mid, Target target, String status, String message) {
		if (tid != 0) {
			this.testId = tid;
		}
		if (mid != 0) {
			this.mutantId = mid;
		}
		this.target = target;
		this.status = status;
		this.message = message;
	}

	// Constructor for TargetExecution retrieved from Database
	public TargetExecution(int id, int tid, int mid, Target target, String status, String message, String timestamp) {
		this(tid, mid, target, status, message);
		this.id = id;
		this.timestamp = Timestamp.valueOf(timestamp);
	}

	public boolean insert() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			logger.debug("Inserting " + toString());
			conn = DB.getConnection();
			if (testId == 0) {
				logger.warn("- No testId");
				if (mutantId == 0) {
					System.out.println("- No mutantId");
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Target, Status, Message) VALUES (?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setString(1, target.name());
					pstmt.setString(2, status);
					pstmt.setString(3, message == null ? "" : message.length() <= 2000 ? message : message.substring(0, 1999));
				} else {
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setInt(1, mutantId);
					pstmt.setString(2, target.name());
					pstmt.setString(3, status);
					pstmt.setString(4, message == null ? "" : message.length() <= 2000 ? message : message.substring(0, 1999));
				}
			} else {
				if (mutantId == 0) {
					System.out.println("- No mutantId");
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Test_ID, Target, Status, Message) VALUES (?, ?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setInt(1, testId);
					pstmt.setString(2, target.name());
					pstmt.setString(3, status);
					pstmt.setString(4, message == null ? "" : message.length() <= 2000 ? message : message.substring(0, 1999));
				} else {
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Test_ID, Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setInt(1, testId);
					pstmt.setInt(2, mutantId);
					pstmt.setString(3, target.name());
					pstmt.setString(4, status);
					pstmt.setString(5, message == null ? "" : message.length() <= 2000 ? message : message.substring(0, 1999));
				}
			}
			pstmt.execute();
			ResultSet rs = pstmt.getGeneratedKeys();

			if (rs.next()) {
				this.id = rs.getInt(1);
				return true;
			}
		} catch (SQLException se) {
			logger.error("Caught SQLException", se);
		} // Handle errors for JDBC
		catch (Exception e) {
			logger.error("Caught Exception", e);
		} // Handle errors for Class.forName
		finally {
			DB.cleanup(conn, pstmt);
		}
		return false;
	}

	@Override
	public String toString() {
		return "TargetExecution{" +
				"id=" + id +
				", testId=" + testId +
				", mutantId=" + mutantId +
				", target='" + target + '\'' +
				", status='" + status + '\'' +
				", message='" + message + '\'' +
				", timestamp=" + timestamp +
				'}';
	}

}