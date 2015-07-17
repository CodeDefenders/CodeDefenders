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
    protected MutationTester mt;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        mt = (MutationTester) getServletContext().getAttribute("gammut.mutationtester");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (gs.isFinished()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("scores");
            dispatcher.forward(request, response);
        }

        int mutantNo = 1;
        for (Mutant m : gs.getAliveMutants()) {
            
            for (diff_match_patch.Diff d : m.getDifferences()) {
                if (d.operation == diff_match_patch.Operation.INSERT) {
                }
                else {
                }
            }
        }

        if (gs.isTurn(DEFENDER)) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/defender_view.jsp");
            dispatcher.forward(request, response);
        }

        else {
            response.setIntHeader("Refresh", 5);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Get the text submitted by the user.
        String testText = request.getParameter("test");
        // Write it to a Java File.
        
        if (createTest(testText, "Book")) {
            mt.runMutationTests(gs.getTests(), gs.getMutants());
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

        // Check the test actually passes when applied to the original code.
        

        if (mt.compileTest(newTest) && mt.testOriginal(newTest)) {
            gs.addTest(newTest);
            return true;
        }

        folder.delete(); 
        return false;
    }
}