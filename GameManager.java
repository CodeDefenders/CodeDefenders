package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;
import java.nio.file.Files;


public class GameManager extends HttpServlet {

    // Based on info provided, navigate to the correct view for the user
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        
        // Initialize MySQL datatypes to null before the try block.
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        // Get the session information specific to the current user.
        HttpSession session = request.getSession();
        int uid = (int)session.getAttribute("uid");
        int gid = (int)session.getAttribute("gid");

        System.out.println("Getting game " + gid + " for " + uid);

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM games WHERE Game_ID='%d'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            // If a game with the provided ID exists, store the data in a Game class and close the connection.
            if (rs.next()) {
                System.out.println("Game with id " + gid + " exists");
                Game activeGame = new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
                                    rs.getInt("CurrentRound"), rs.getInt("FinalRound"), rs.getString("ActivePlayer"), rs.getString("State"));
                
                stmt.close();
                conn.close();

                // If the game is finished, redirect to the score page. No uid checking needed, anyone can view.
                if (activeGame.getState().equals("FINISHED")) {
                    session.setAttribute("game", activeGame);
                    RequestDispatcher dispatcher = request.getRequestDispatcher("html/score_view.jsp");
                    dispatcher.forward(request, response);
                }

                // If the current user is one of the players in the game
                if (activeGame.getAttackerId() == uid) {
                    System.out.println("user is attacker");
                    session.setAttribute("game", activeGame);

                    for (Mutant m : getMutantsForGame(activeGame.getId())) {
                        // If at least one mutant needs to be proved non-equivalent, go to the Resolve Equivalence page.
                        if (m.isEquivalent() && m.isAlive()) {
                            RequestDispatcher dispatcher = request.getRequestDispatcher("html/resolve_equivalence.jsp");
                            dispatcher.forward(request, response);
                        }
                    }
                    
                    System.out.println("Should be going to attacker page");
                    // If no mutants needed to be proved non-equivalent, direct to the Attacker Page.
                    RequestDispatcher dispatcher = request.getRequestDispatcher("html/attacker_view.jsp");
                    dispatcher.forward(request, response);
                }

                if (activeGame.getDefenderId() == uid) {
                    session.setAttribute("game", activeGame);
                    // Direct to the Defender Page.
                    RequestDispatcher dispatcher = request.getRequestDispatcher("html/defender_view.jsp");
                    dispatcher.forward(request, response);
                }
            }
            else {
                stmt.close();
                conn.close();
            }
    
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }

        response.sendRedirect(request.getHeader("referer"));
    } 

    // Based on the data provided, update information for the game
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        Game activeGame = (Game)request.getSession().getAttribute("game");

        switch (request.getParameter("formType")) {
            
            case "resolveEquivalence" :
                /*
                // Check type of equivalence response.
                if (request.getParameter("supplytest").equals("true")) {
                    Test test = null;
                    Mutant mutant = null;
                    // Get the text submitted by the user.
                    String testText = request.getParameter("test");
                    // Write it to a Java File.
                    
                    if ((test = createTest(testText, gs.getClassName())) != null) {
                        for (Mutant m : gs.getMutants()) {
                            if (m.isEquivalent() && m.isAlive()) {
                                mutant = m;
                                break;
                            }
                        }

                        MutationTester.runEquivalenceTest(test, mutant, gs.getClassName());
                    }
                }
                else {
                    for (Mutant m : gs.getMutants()) {
                        if (m.isEquivalent() && m.isAlive()) {
                            m.setAlive(false);
                            m.removePoints();
                            break;
                        }
                    }
                }*/
                break;

            case "markEquivalence" :
                /*
                int count = 0;
                for (Mutant m : gs.getMutants()) {
                    System.out.println(request.getParameter("mutant"+count));
                    if (request.getParameter("mutant"+count) != null) {
                        m.setEquivalent(true);
                    }
                }*/
                break;

            case "createMutant" :
                /*
                // Get the text submitted by the user.
                String mutantText = request.getParameter("mutant");

                // If it can be written to file and compiled, end turn. Otherwise, dont.
                if (createMutant(activeGame.getGameId(), activeGame.getClassId(), mutantText)) {
                    gs.endTurn();
                }*/
                break;

            case "createTest" :
                /*
                // Get the text submitted by the user.
                String testText = request.getParameter("test");
                // Write it to a Java File.
                
                if (createTest(testText, gs.getClassName())) {
                    mt.runMutationTests(gs.getTests(), gs.getMutants(), gs.getClassName());
                    gs.endTurn();
                }*/
                break;
        }

        doGet(request, response);
    }

    public static ArrayList<Mutant> getMutantsForGame(int gid) {

        ArrayList<Mutant> mutList = new ArrayList<Mutant>();
        /*
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM mutants WHERE Game_ID='%i'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"), rs.getBlob("JavaFile").getBytes(), rs.getBlob("ClassFile").getBytes(), 
                              rs.getBoolean("Alive"), rs.getBoolean("SuspectEquivalent"), rs.getBoolean("DeclaredEquivalent"), rs.getInt("Points"));
                mutList.add(newMutant);
            }

            stmt.close();
            conn.close();
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }
        */
        return mutList;
    }

    // Writes text as a Mutant to the appropriate place in the file system.
    public boolean createMutant(int gameId, int classId, String mutantText) throws IOException {

        /*

        // First get the class name from the class ID.
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        String className;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM classes WHERE Class_ID='%i'", classId);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {className = rs.getString("Name");}

            stmt.close();
            conn.close();
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
            return false;
        }

        String original = "";
        String line;

        // Loads the original code that the mutant was created from into a String variable.
        InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/resources/"+className+".java");
        BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
        while((line = is.readLine()) != null) {original += line + "\n";}

        // Runs diff match patch between the two Strings to see if there are any differences.
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> changes = dmp.diff_main(original.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
        boolean noChange = true;
        for (diff_match_patch.Diff d : changes) {
            if (d.operation != diff_match_patch.Operation.EQUAL) {
                noChange = false;
            }
        }

        // If there were no differences, return, as the mutant is the same as original.
        if (noChange) {return false;}

        // Setup folder the files will go in
        Long timestamp = System.currentTimeMillis();
        File folder = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+timestamp));
        folder.mkdir();

        // Write the String into a java file
        File mutant = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+timestamp+"/"+name+".java"));
        FileWriter fw = new FileWriter(mutant);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(mutantText);
        bw.close();

        // Try and compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.

        if (MutationTester.compileMutant(folder, name)) {

            byte[] javaFile = Files.readAllBytes(getServletContext().getRealPath("/WEB-INF/mutants/"+timestamp+"/"+className+".java"));
            byte[] classFile = Files.readAllBytes(getServletContext().getRealPath("/WEB-INF/mutants/"+timestamp+"/"+className+".class"));

            Mutant newMutant = new Mutant(gameId, javaFile, classFile);
            newMutant.insertMutant(); 
            folder.delete(); 
            return true;
        }
        else {folder.delete(); return false;}

        */
        return false;
    }

    public static ArrayList<Test> getTestsForGame(int gid) {
        ArrayList<Test> testList = new ArrayList<Test>();
        /*
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM mutants WHERE Game_ID='%i'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"), rs.getBlob("JavaFile").getBytes(), rs.getBlob("ClassFile").getBytes(), 
                              rs.getBoolean("Alive"), rs.getBoolean("SuspectEquivalent"), rs.getBoolean("DeclaredEquivalent"), rs.getInt("Points"));
                mutList.add(newMutant);
            }

            stmt.close();
            conn.close();
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }
        */
        return testList;
    }

    // create mutant method

    public static File getJavaFile(int cid) {
        File nullFile = null;
        return nullFile;
    }

    public static File getClassFile(int cid) {
        File nullFile = null;
        return nullFile;
    }
}