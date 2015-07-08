import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ArrayList;

public class DefenderPage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    String testError = "";

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        GameState gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Defender Window</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<p>"+testError+"</p>");
        out.println("<p>Scores are currently Attacker: "+gs.getScore(ATTACKER)+", Defender: "+gs.getScore(DEFENDER)+"</p>");
        out.println("<p>Round is: "+gs.getRound()+"</p>");
        out.println("<p>There are "+gs.getAliveMutants().size()+" mutants alive </p>");

        if (gs.isTurn(DEFENDER)) {

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

        out.println("</body>");
        out.println("</html>");

    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        
        // Open GameState for storing new data.
        GameState gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        // Get the text submitted by the user.
        String testText = request.getParameter("test");
        // Write it to a Java File.
        Test newTest = writeTestToJava(testText, "Book");
        // Add a record of the mutant to the Game State.
        gs.addTest(newTest);
        // Set the GameState.
        getServletContext().setAttribute("gammut.gamestate", gs);

        MutationTester mt = (MutationTester)getServletContext().getAttribute("gammut.mutationtester");
        testError = mt.runMutationTests(gs.getTests(), gs.getMutants());

        gs.endTurn();

        // Display as if get request received.
        doGet(request, response);
    }

    // Writes text as a Test to the appropriate place in the file system.
    public Test writeTestToJava(String testText, String name) throws IOException {
        Long timestamp = System.currentTimeMillis();
        File folder = new File(getServletContext().getRealPath("/WEB-INF/tests/"+timestamp));
        folder.mkdir();

        File test = new File(getServletContext().getRealPath("/WEB-INF/tests/"+timestamp+"/Test"+name+".java"));
        FileWriter fw = new FileWriter(test);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(testText);
        bw.close();
        return new Test(folder, name);
    }
}