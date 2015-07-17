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

        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Defender Window</title>");
        out.println("</head>");
        out.println("<body>");

        int mutantNo = 1;
        for (Mutant m : gs.getAliveMutants()) {

            out.println("<h4> Mutant Number " + mutantNo + "</h4>");
            for (diff_match_patch.Diff d : m.getDifferences()) {
                if (d.operation == diff_match_patch.Operation.INSERT) {
                    out.println("<p>Added: " + d.text+"</p>");
                }
                else {
                    out.println("<p>Removed: " + d.text+"</p>");
                }
            }
        }

        out.println("<p>Scores are currently Attacker: "+gs.getScore(ATTACKER)+", Defender: "+gs.getScore(DEFENDER)+"</p>");
        out.println("<p>Round is: "+gs.getRound()+"</p>");
        out.println("<p>There are "+gs.getAliveMutants().size()+" mutants alive </p>");

        if (gs.isTurn(DEFENDER)) {

            InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/resources/Book.java");

            out.println("<textarea name=\"source\" cols=\"100\" rows=\"50\" readonly>");
            String line;
            BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
            while((line = is.readLine()) != null)
                out.println(line);
            out.println("</textarea>");

            out.println("<form action=\"/gammut/defender\" method=\"post\">");
            out.println("<input type=\"hidden\" name=\"user\" value=\"1\">");
            out.println("<textarea name=\"test\" cols=\"100\" rows=\"30\">");
            out.println("import org.junit.*;");
            out.println("import static org.junit.Assert.*;");
            out.println("");
            out.println("public class TestBook {");
            out.println("  @Test");
            out.println("  public void test() {");
            out.println("");
            out.println("  }");
            out.println("}");
            out.println("</textarea>");
            out.println("<br><input type=\"submit\" value=\"Defend!\">");
            out.println("</form>");

        }

        else {

            response.setIntHeader("Refresh", 5);
            out.println("<h1>Waiting for attacker input.</h1>");

        }

        out.println("<p>"+mt.getLog()+"</p>");
        out.println("</body>");
        out.println("</html>");

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