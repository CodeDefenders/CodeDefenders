package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;

public class GameSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/user_games_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Get their user id from the session.
        int uid = (Integer)request.getSession().getAttribute("uid");

        switch (request.getParameter("formType")) {
            
            case "createGame" :

                // Get the identifying information required to create a game from the submitted form.
                int classId = Integer.parseInt((String)request.getParameter("class"));
                int rounds = Integer.parseInt((String)request.getParameter("rounds"));
                String role = (String)request.getParameter("role");

                

                System.out.println("about to create game");
                if (createGame(classId, uid, rounds, role)) {
                    System.out.println("created game successfully");
                    // redirect to the individual game.
                }
                else {
                    System.out.println("creating game failed");
                    // redirect to the create game page with error.
                }
                
                break;

            case "joinGame" :

                // Get the identifying information required to create a game from the submitted form.
                int gameId = Integer.parseInt((String)request.getParameter("game"));

                if (joinGame(gameId, uid)) {
                    // redirect to the individual game.
                }
                else {
                    // redirect to the previous page with error.
                }

                break;
        }

        doGet(request, response);
    }

    public static ArrayList<Game> getGamesForUser(int userId) {
        Connection conn = null;
        Statement stmt = null;
        String sql = null;
        ArrayList<Game> gameList = new ArrayList<Game>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM games WHERE Attacker_ID=%d OR Defender_ID=%d;", userId, userId);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                gameList.add(new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
                    rs.getInt("CurrentRound"), rs.getInt("FinalRound"), rs.getString("ActivePlayer"), rs.getString("State")));
            }

            stmt.close();
            conn.close();
            

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

        return gameList;
    }

    public static ArrayList<Game> getAllGames() {
        Connection conn = null;
        Statement stmt = null;
        String sql = null;
        ArrayList<Game> gameList = new ArrayList<Game>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = "SELECT * FROM games;";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                gameList.add(new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
                    rs.getInt("CurrentRound"), rs.getInt("FinalRound"), rs.getString("ActivePlayer"), rs.getString("State")));
            }

            stmt.close();
            conn.close();
            

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

        return gameList;
    }

    public static boolean createGame(int classId, int userId, int maxRounds, String role) {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        // Attempt to insert game info into database
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("INSERT INTO games (%s, FinalRound, Class_ID) VALUES ('%d', '%d', '%d');", role, userId, maxRounds, classId);
            stmt.execute(sql);

            stmt.close();
            conn.close();
            System.out.println("createGame: successfully created game");
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
                if(conn!=null)
                conn.close();
            } catch(SQLException se) {
                System.out.println(se);
            }//end finally try
        } //end try

        System.out.println("createGame: failed to create game");
        return false;
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
}