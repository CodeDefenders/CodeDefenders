package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;

public class LoginManager extends HttpServlet {

    GameState gs;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        gs = (GameState)getServletContext().getAttribute("gammut.gamestate");

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        String username = (String)request.getParameter("username");
        String password = (String)request.getParameter("password");
        String action = (String)request.getParameter("action");
        int uid;

        System.out.println("Received form with "+username+password+action);

        if (action.equals("create")) {
            String confirm = (String)request.getParameter("confirm");
            System.out.println("Try to create");
            if ((uid = createAccount(username, password, confirm)) != -1) {
                System.out.println("success");
                HttpSession session = request.getSession();
                session.setAttribute("uid", uid);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(1200);

                RequestDispatcher dispatcher = request.getRequestDispatcher("html/user_games_view.jsp");
                dispatcher.forward(request, response);
            }
            else {
                RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
                dispatcher.forward(request, response);
            }
        }

        else if (action.equals("login")) {
            if ((uid = loginAccount(username, password)) != -1) {
                HttpSession session = request.getSession();
                session.setAttribute("uid", uid);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(1200);

                RequestDispatcher dispatcher = request.getRequestDispatcher("html/user_games_view.jsp");
                dispatcher.forward(request, response);
            }
            else {
                RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
                dispatcher.forward(request, response);
            }
        }

        else {
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
            dispatcher.forward(request, response);
        }
    }

    public int createAccount(String username, String password, String confirm) {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        // Check to make sure password and confirmed password are the same
        if (password.equals(confirm)) {

            try {
                System.out.println("starting to query");
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);
                System.out.println("got connection");

                // Get all rows from the database which have the chosen username
                stmt = conn.createStatement();
                sql = String.format("SELECT * FROM users WHERE username='%s'", username);
                ResultSet rs = stmt.executeQuery(sql);
                System.out.println("executed statement");

                // If no rows had the username, it is not in use and data can be stored.
                if (!rs.next()) {
                    stmt = conn.createStatement();
                    sql = String.format("INSERT INTO users (Username, Password) VALUES ('%s', '%s');", username, password);
                    stmt.execute(sql);

                    stmt = conn.createStatement();
                    sql = String.format("SELECT * FROM users WHERE username='%s'", username);
                    rs = stmt.executeQuery(sql);
                    rs.next();
                    int userId = rs.getInt("User_ID");

                    stmt.close();
                    conn.close();
                    return userId;
                }
                

            }
            catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
            catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
            finally {
                try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
                try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
            }

        }
        return -1;
    }

    public int loginAccount(String username, String password) {
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            // Get all rows from the database which have the chosen username
            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM users WHERE username='%s'", username);
            ResultSet rs = stmt.executeQuery(sql);

            // If the username exists in the db, compare entered password to existing and return true if matches
            if (rs.next()) {
                if (rs.getString("password").equals(password)) {
                    int userId = rs.getInt("User_ID");
                    stmt.close();
                    conn.close();
                    return userId;
                }
            }
            

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
            se.printStackTrace();
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally{
            //finally block used to close resources
            try {
                if(stmt!=null)
                   stmt.close();
            } catch(SQLException se2) {}// nothing we can do

            try {
                if(conn!=null)
                conn.close();
            } catch(SQLException se) {
                se.printStackTrace();
            }//end finally try
        } //end try

        return -1;
    }
}