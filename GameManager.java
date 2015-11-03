package gammut;

import java.nio.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;
import java.nio.file.Files;
import diff_match_patch.*;

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
                if (request.getParameter("supplyTest").equals("true")) {
                    Test test = null;
                    Mutant mutant = null;
                    // Get the text submitted by the user.
                    String testText = request.getParameter("test");

                    // If it can be written to a Java file.
                    if ((test = createTest(testText, gs.getClassName())) != null) {
                        for (Mutant m : activeGame.getMutants()) {
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
                }
                break;

            case "markEquivalence" :

                int count = 0;
                for (Mutant m : gs.getMutants()) {
                    System.out.println(request.getParameter("mutant"+count));
                    if (request.getParameter("mutant"+count) != null) {
                        m.setEquivalent(true);
                    }
                }
                break;

            */

            case "createMutant" :

                // Get the text submitted by the user.
                String mutantText = request.getParameter("mutant");

                // If it can be written to file and compiled, end turn. Otherwise, dont.
                if (createMutant(activeGame.getId(), activeGame.getClassId(), mutantText)) {
                    activeGame.endTurn();
                    activeGame.update();
                }
                break;

            case "createTest" :
                
                // Get the text submitted by the user.
                String testText = request.getParameter("test");

                // If it can be written to file and compiled, end turn. Otherwise, dont.
                if (createTest(activeGame.getId(), activeGame.getClassId(), testText)) {
                    //MutationTester.runMutationTests(activeGame.getTests(), activeGame.getMutants(), activeGame.getClassName());
                    activeGame.endTurn();
                    activeGame.update();
                }
                break;
        }

        doGet(request, response);
    }

    public static ArrayList<Mutant> getMutantsForGame(int gid) {

        ArrayList<Mutant> mutList = new ArrayList<Mutant>();
        
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM mutants WHERE Game_ID='%d'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"), 
                                   rs.getString("JavaFile"), rs.getString("ClassFile"), 
                                   rs.getBoolean("Alive"), rs.getBoolean("SuspectEquivalent"), rs.getBoolean("DeclaredEquivalent"), 
                                   rs.getInt("RoundCreated"), rs.getInt("RoundKilled"));
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
        
        return mutList;
    }

    public static ArrayList<Test> getTestsForGame(int gid) {
        ArrayList<Test> testList = new ArrayList<Test>();
        
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT * FROM tests WHERE Game_ID='%d'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Test newTest = new Test(rs.getInt("Test_ID"), rs.getInt("Game_ID"), 
                                   rs.getString("JavaFile"), rs.getString("ClassFile"), 
                                   rs.getInt("RoundCreated"), rs.getInt("MutantsKilled"));
                testList.add(newTest);
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
        
        return testList;
    }

    // Writes text as a Mutant to the appropriate place in the file system.
    public boolean createMutant(int gid, int cid, String mutantText) throws IOException {

        String className = GameSelectionManager.getNameForClass(cid);

        File sourceFile = getJavaFileForClass(cid);
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

        // Runs diff match patch between the two Strings to see if there are any differences.
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> changes = dmp.diff_main(sourceCode.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
        boolean noChange = true;
        for (diff_match_patch.Diff d : changes) {
            if (d.operation != diff_match_patch.Operation.EQUAL) {
                noChange = false;
            }
        }

        // If there were no differences, return, as the mutant is the same as original.
        if (noChange) {return false;}

        // Setup folder the files will go in
        File folder = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+gid));
        folder.mkdir();

        // Write the Mutant String into a java file
        File mutant = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+gid+"/"+className+".java"));
        FileWriter fw = new FileWriter(mutant);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(mutantText);
        bw.close();

        // Try and compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.

        if (MutationTester.compileMutant(folder, className)) {

            String jFile = getServletContext().getRealPath("/WEB-INF/mutants/"+gid+"/"+className+".java");
            String cFile = getServletContext().getRealPath("/WEB-INF/mutants/"+gid+"/"+className+".class");

            Mutant newMutant = new Mutant(gid, jFile, cFile);
            newMutant.insert();

            return true;
        }
        else {mutant.delete(); return false;}
    }

    public boolean createTest(int gid, int cid, String testText) throws IOException {

        String className = GameSelectionManager.getNameForClass(cid);

        File sourceFile = getJavaFileForClass(cid);
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

        File folder = new File(getServletContext().getRealPath("/WEB-INF/tests/"+gid));
        folder.mkdir();

        File test = new File(getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/Test"+className+".java"));
        FileWriter testWriter = new FileWriter(test);
        BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
        bufferedTestWriter.write(testText);
        bufferedTestWriter.close();

        File source = new File(getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/"+className+".java"));
        FileWriter sourceWriter = new FileWriter(source);
        BufferedWriter bufferedSourceWriter = new BufferedWriter(sourceWriter);
        bufferedSourceWriter.write(sourceCode);
        bufferedSourceWriter.close();

        // Check the test actually passes when applied to the original code.
        
        if (MutationTester.compileTest(folder, className) && MutationTester.testOriginal(folder, className)) {

            String jFile = getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/Test"+className+".java");
            String cFile = getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/Test"+className+".class");

            Test newTest = new Test(gid, jFile, cFile);
            newTest.insert();
            return true;
        }

        test.delete();
        source.delete();
        return false;
    }

    public static File getJavaFileForClass(int cid) {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);
            stmt = conn.createStatement();
            sql = String.format("SELECT JavaFile FROM classes WHERE Class_ID=%d;", cid);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                File javaFile = new File(rs.getString("JavaFile"));
                stmt.close();
                conn.close();
                return javaFile;
            }

            stmt.close();
            conn.close();
            

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
        return null;
    }

    public static File getClassFileForClass(int cid) {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);
            stmt = conn.createStatement();
            sql = String.format("SELECT ClassFile FROM classes WHERE Class_ID=%d;", cid);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                File classFile = new File(rs.getString("ClassFile"));

                stmt.close();
                conn.close();
                return classFile;
            }

            stmt.close();
            conn.close();
            

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
        return null;
    }

    public static int getClassForGame(int gid) {

        int classId = -1;
        
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT Class_ID FROM games WHERE Game_ID='%d'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                classId = rs.getInt("Class_ID");
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
        
        return classId;
    }

    public static int getCurrentRoundForGame(int gid) {

        int currentRound = -1;
        
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            // Load the Game Data with the provided ID.
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("SELECT CurrentRound FROM games WHERE Game_ID='%d'", gid);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                currentRound = rs.getInt("CurrentRound");
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
        
        return currentRound;
    }
}