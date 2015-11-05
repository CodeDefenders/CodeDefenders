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

        // Get the session information specific to the current user.
        HttpSession session = request.getSession();
        int uid = (int)session.getAttribute("uid");
        int gid = (int)session.getAttribute("gid");

        System.out.println("Getting game " + gid + " for " + uid);

        Game activeGame = DatabaseAccess.getGameForKey("Game_ID", gid);

        // If the game is finished, redirect to the score page.
        if (activeGame.getState().equals("FINISHED")) {
            session.setAttribute("game", activeGame);
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/score_view.jsp");
            dispatcher.forward(request, response);
        }

        if (activeGame.getAttackerId() == uid) {
            System.out.println("user is attacker");
            session.setAttribute("game", activeGame);

            for (Mutant m : DatabaseAccess.getMutantsForGame(activeGame.getId())) {
                // If at least one mutant needs to be proved non-equivalent, go to the Resolve Equivalence page.
                System.out.println("about to check if a mutant is equiv");
                if (m.getEquivalent().equals("PENDING_TEST") && m.isAlive()) {
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
                    MutationTester.runMutationTests(activeGame.getId());
                    activeGame.endTurn();
                    activeGame.update();
                }
                break;
        }

        doGet(request, response);
    }

    // Writes text as a Mutant to the appropriate place in the file system.
    public boolean createMutant(int gid, int cid, String mutantText) throws IOException {

        GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);

        File sourceFile = new File(classMutated.javaFile);
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
        File mutant = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+gid+"/"+classMutated.name+".java"));
        FileWriter fw = new FileWriter(mutant);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(mutantText);
        bw.close();

        // Try and compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.

        if (MutationTester.compileMutant(folder, classMutated.name)) {

            String jFile = getServletContext().getRealPath("/WEB-INF/mutants/"+gid+"/"+classMutated.name+".java");
            String cFile = getServletContext().getRealPath("/WEB-INF/mutants/"+gid+"/"+classMutated.name+".class");

            Mutant newMutant = new Mutant(gid, jFile, cFile);
            newMutant.insert();

            return true;
        }
        else {mutant.delete(); return false;}
    }

    public boolean createTest(int gid, int cid, String testText) throws IOException {

        GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

        File sourceFile = new File(classUnderTest.javaFile);
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

        File folder = new File(getServletContext().getRealPath("/WEB-INF/tests/"+gid));
        folder.mkdir();

        File test = new File(getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/Test"+classUnderTest.name+".java"));
        FileWriter testWriter = new FileWriter(test);
        BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
        bufferedTestWriter.write(testText);
        bufferedTestWriter.close();

        // Check the test actually passes when applied to the original code.
        
        if (MutationTester.compileTest(folder, classUnderTest.name) && MutationTester.testOriginal(folder, classUnderTest.name)) {

            String jFile = getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/Test"+classUnderTest.name+".java");
            String cFile = getServletContext().getRealPath("/WEB-INF/tests/"+gid+"/Test"+classUnderTest.name+".class");

            Test newTest = new Test(gid, jFile, cFile);
            newTest.insert();
            return true;
        }

        test.delete();
        return false;
    }
}