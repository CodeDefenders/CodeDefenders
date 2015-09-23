package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ArrayList;
import diff_match_patch.*;

public class DefenderPage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    String testError = "";

    protected GameState gs;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");

        if (gs.isFinished()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("scores");
            dispatcher.forward(request, response);
        }

        if (gs.isTurn(DEFENDER)) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/defender_view.jsp");
            dispatcher.forward(request, response);
        }

        else {
            response.setIntHeader("Refresh", 5);
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/idle_view.jsp");
            dispatcher.forward(request, response);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        int count = 0;
        for (Mutant m : gs.getMutants()) {
            System.out.println(request.getParameter("mutant"+count));
            if (request.getParameter("mutant"+count) != null) {
                m.setEquivalent(true);
            }
        }

        // Get the text submitted by the user.
        String testText = request.getParameter("test");
        // Write it to a Java File.
        
        if (createTest(testText, gs.getClassName())) {
            MutationTester.runMutationTests(gs.getTests(), gs.getMutants(), gs.getClassName());
            gs.endTurn();
        }

        // Display as if get request received.
        doGet(request, response);
    }

    // Writes text as a Test to the appropriate place in the file system.
    public boolean createTest(String testText, String name) throws IOException {

        Long timestamp = System.currentTimeMillis();
        File folder = new File(getServletContext().getRealPath("/WEB-INF/tests/"+timestamp));
        folder.mkdir();

        File test = new File(getServletContext().getRealPath("/WEB-INF/tests/"+timestamp+"/Test"+name+".java"));
        FileWriter fw = new FileWriter(test);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(testText);
        bw.close();

        Test newTest = new Test(folder, name);
        newTest.setText(testText);

        // Check the test actually passes when applied to the original code.
        

        if (MutationTester.compileTest(newTest, name) && MutationTester.testOriginal(newTest, name)) {
            gs.addTest(newTest);
            return true;
        }

        folder.delete();
        return false;
    }
}