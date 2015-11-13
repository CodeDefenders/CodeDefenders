package gammut;

import java.sql.*;

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
		if (tid != 0) {this.testId = tid;}
		if (mid != 0) {this.mutantId = mid;}
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
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();

            if (testId == 0) {
            	if (mutantId == 0) {
            		sql = String.format("INSERT INTO targetexecutions (Target, Status, Message) VALUES ('%s', '%s', '%s');", target, status, message);
            	}
            	else {
            		sql = String.format("INSERT INTO targetexecutions (Mutant_ID, Target, Status, Message) VALUES ('%d', '%s', '%s', '%s');", mutantId, target, status, message);
            	}
            }
            else {
            	if (mutantId == 0) {
            		sql = String.format("INSERT INTO targetexecutions (Test_ID, Target, Status, Message) VALUES ('%d', '%s', '%s', '%s');", testId, target, status, message);
            	}
            	else {
            		sql = String.format("INSERT INTO targetexecutions (Test_ID, Mutant_ID, Target, Status, Message) VALUES ('%d', '%d', '%s', '%s', '%s');", testId, mutantId, target, status, message);
            	}
            }

            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                id = rs.getInt(1);
                stmt.close();
                conn.close();
                return true;
            }
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }
        return false;
	}
}