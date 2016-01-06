package org.gammut;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TargetExecution {

	public int id;
	public int testId;
	public int mutantId;
	public String target;
	public String status;
	public String message;
	public Timestamp timestamp;

	// Constructors for inital creation of TargetExecution

	public TargetExecution(int tid, int mid, String target, String status, String message) {
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
	public TargetExecution(int id, int tid, int mid, String target, String status, String message, String timestamp) {
		this(tid, mid, target, status, message);
		this.id = id;
		this.timestamp = Timestamp.valueOf(timestamp);
	}

	public boolean insert() {

		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = null;

		try {
			System.out.println("inserting targetexecution");
			System.out.println(testId + " " + mutantId + target + status + message);

			conn = DatabaseAccess.getConnection();

			if (testId == 0) {
				if (mutantId == 0) {
					System.out.println(target + status + message);
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Target, Status, Message) VALUES (?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setString(1, target);
					pstmt.setString(2, status);
					pstmt.setString(3, message);
				} else {
					System.out.println(mutantId + target + status + message);
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setInt(1, mutantId);
					pstmt.setString(2, target);
					pstmt.setString(3, status);
					pstmt.setString(4, message);
				}
			} else {
				if (mutantId == 0) {
					System.out.println(testId + target + status + message);
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Test_ID, Target, Status, Message) VALUES (?, ?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setInt(1, testId);
					pstmt.setString(2, target);
					pstmt.setString(3, status);
					pstmt.setString(4, message);
				} else {
					System.out.println(testId + " " + mutantId + target + status + message);
					pstmt = conn.prepareStatement("INSERT INTO targetexecutions (Test_ID, Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?, ?);", new String[]{"TargetExecution_ID"});
					pstmt.setInt(1, testId);
					pstmt.setInt(2, mutantId);
					pstmt.setString(3, target);
					pstmt.setString(4, status);
					pstmt.setString(5, message);
				}
			}

			pstmt.execute();
			System.out.println("executing the statement");

			ResultSet rs = pstmt.getGeneratedKeys();


			if (rs.next()) {
				System.out.println("trying to get the key for ID");
				this.id = rs.getInt(1);
				System.out.println(this.id);
				pstmt.close();
				conn.close();
				return true;
			}
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return false;
	}
}