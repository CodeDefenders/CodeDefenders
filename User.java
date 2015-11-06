package gammut;

import java.sql.*;

public class User {

	public int id;
	public String username;
	public String password;

	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public User(int id, String username, String password) {
		this(username, password);
		this.id = id;
	}

	public boolean insert() {

		Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("INSERT INTO users (Username, Password) VALUES ('%s', '%s');", username, password);

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