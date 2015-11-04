package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;

public class GameSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.sendRedirect("games/user");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Get their user id from the session.
        int uid = (Integer)request.getSession().getAttribute("uid");
        int gameId;

        switch (request.getParameter("formType")) {
            
            case "createGame" :

                // Get the identifying information required to create a game from the submitted form.
                int classId = Integer.parseInt((String)request.getParameter("class"));
                int rounds = Integer.parseInt((String)request.getParameter("rounds"));
                String role = (String)request.getParameter("role");

                System.out.println("about to create game");
                if ((gameId = createGame(classId, uid, rounds, role)) != -1) {

                    HttpSession session = request.getSession();
                    session.setAttribute("gid", gameId);

                    response.sendRedirect("play");
                }

                else {
                    response.sendRedirect(request.getHeader("referer"));
                }
                
                break;

            case "joinGame" :

                // Get the identifying information required to create a game from the submitted form.
                gameId = Integer.parseInt((String)request.getParameter("game"));

                if (joinGame(gameId, uid)) {

                    HttpSession session = request.getSession();
                    session.setAttribute("gid", gameId);

                    response.sendRedirect("play");
                }

                else {
                    response.sendRedirect(request.getHeader("referer"));
                }

                break;

            case "enterGame" :

                gameId = Integer.parseInt((String)request.getParameter("game"));

                if (canEnterGame(gameId, uid)) {

                    HttpSession session = request.getSession();
                    session.setAttribute("gid", gameId);
                    
                    response.sendRedirect("play");
                }

                else {response.sendRedirect(request.getHeader("referer"));}

                break;
        }
    }

    public static int createGame(int classId, int userId, int maxRounds, String role) {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        // Attempt to insert game info into database
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("INSERT INTO games (%s, FinalRound, Class_ID) VALUES ('%d', '%d', '%d');", role, userId, maxRounds, classId);
            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                int gameId = rs.getInt(1);
                stmt.close();
                conn.close();
                System.out.println("createGame: successfully created game with id: " + gameId);
                return gameId;
            }

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
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
                System.out.println(se);
            }//end finally try
        } //end try

        System.out.println("createGame: failed to create game");
        return -1;
    }

    public static boolean joinGame(int gameId, int userId) {
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        String role = null;

        // Attempt to insert game info into database
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT Attacker_ID, Defender_ID FROM games WHERE Game_ID=%d;", gameId);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                if (rs.getInt("Attacker_ID") == 0) {role = "Attacker_ID";}
                else if (rs.getInt("Defender_ID") == 0) {role = "Defender_ID";}
                else {System.out.println("joinGame: neither attacker or defender was null"); return false;}
            }

            stmt = conn.createStatement();
            sql = String.format("UPDATE games SET %s=%d, State='IN PROGRESS', ActivePlayer='ATTACKER' WHERE Game_ID=%d;", role, userId, gameId);
            stmt.execute(sql);
            System.out.println("joinGame: successfully added user to game");

            stmt.close();
            conn.close();
            
            return true;

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
        } finally{
            //finally block used to close resources
            try {
                if(stmt!=null)
                   stmt.close();
            } catch(SQLException se2) {}// nothing we can do

            try {
                if(conn!=null);
                conn.close();
            } catch(SQLException se) {
                System.out.println(se);
            }//end finally try
        } //end try

        System.out.println("joinGame: failed to add user to game");
        return false;
    }

    public static boolean canEnterGame(int gameId, int uid) {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        // Attempt to insert game info into database
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT Attacker_ID, Defender_ID FROM games WHERE Game_ID=%d;", gameId);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                if ((rs.getInt("Attacker_ID") == uid)||(rs.getInt("Defender_ID") == uid)) {
                    stmt.close();
                    conn.close();
                    return true;
                }
                else {
                    System.out.println("canEnterGame: not playing in the current game");
                    stmt.close();
                    conn.close();
                    return false;
                }
            }
            else {
                System.out.println("canEnterGame: no games with id: " + gameId);
                stmt.close();
                conn.close();
                return false;
            }

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
        } finally{
            //finally block used to close resources
            try {
                if(stmt!=null)
                   stmt.close();
            } catch(SQLException se2) {}// nothing we can do

            try {
                if(conn!=null);
                conn.close();
            } catch(SQLException se) {
                System.out.println(se);
            }//end finally try
        } //end try

        System.out.println("canEnterGame: failed to enter game");
        return false;
    }
}