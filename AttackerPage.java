package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import diff_match_patch.*;

public class AttackerPage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    protected GameState gs;
    protected MutationTester mt;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mt = (MutationTester) getServletContext().getAttribute("gammut.mutationtester");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");

        if (gs.isFinished()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("scores");
            dispatcher.forward(request, response);
        }

        if (gs.isTurn(ATTACKER)) {
            for (Mutant m : gs.getMutants()) {
                if (m.isEquivalent() && m.isAlive()) {
                    RequestDispatcher dispatcher = request.getRequestDispatcher("html/resolve_equivalence.jsp");
                    dispatcher.forward(request, response);
                }
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher("html/attacker_view.jsp");
            dispatcher.forward(request, response);
        }

        else {
            response.setIntHeader("Refresh", 5);
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/idle_view.jsp");
            dispatcher.forward(request, response);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (request.getParameter("supplytest") != null) {
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

                    mt.runEquivalenceTest(test, mutant, gs.getClassName());
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
            
        }

        else {
            // Get the text submitted by the user.
            String mutantText = request.getParameter("mutant");

            // If it can be written to file and compiled, end turn. Otherwise, dont.
            if (createMutant(mutantText, gs.getClassName())) {
                gs.endTurn();
            }        
        }

        doGet(request, response);
    }

    // Writes text as a Mutant to the appropriate place in the file system.
    private boolean createMutant(String mutantText, String name) throws IOException {

        String original = "";
        String line;

        InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/resources/"+name+".java");
        BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
        while((line = is.readLine()) != null) {original += line + "\n";}

        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> changes = dmp.diff_main(original.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
        boolean noChange = true;
        for (diff_match_patch.Diff d : changes) {
            if (d.operation != diff_match_patch.Operation.EQUAL) {
                noChange = false;
            }
        }

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
        Mutant newMutant = new Mutant(folder, name);
        newMutant.setDifferences(changes);

        if (mt.compileMutant(newMutant, name)) {gs.addMutant(newMutant); return true;}
        else {folder.delete(); return false;}
    }

    // Writes text as a Test to the appropriate place in the file system.
    public Test createTest(String testText, String name) throws IOException {

        Long timestamp = System.currentTimeMillis();
        File folder = new File(getServletContext().getRealPath("/WEB-INF/tests/"+timestamp));
        folder.mkdir();

        File test = new File(getServletContext().getRealPath("/WEB-INF/tests/"+timestamp+"/Test"+name+".java"));
        FileWriter fw = new FileWriter(test);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(testText);
        bw.close();

        Test newTest = new Test(folder, name);

        // Check the test actually passes when applied to the original code.
        

        if (mt.compileTest(newTest, name) && mt.testOriginal(newTest, name)) {
            gs.addTest(newTest);
            return newTest;
        }

        folder.delete();
        return null;
    }
}