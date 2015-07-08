import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.*;

public class AttackerPage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    GameState gs;
    MutationTester mt;

    String diffLog = "";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        mt = (MutationTester) getServletContext().getAttribute("gammut.mutationtester");

        if (gs.isFinished()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("scores");
            dispatcher.forward(request, response);
        }

        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Attacker Window</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<p>"+diffLog+"</p>");

        out.println("<p>Scores are currently Attacker: "+gs.getScore(ATTACKER)+", Defender: "+gs.getScore(DEFENDER)+"</p>");
        out.println("<p>Round is: "+gs.getRound()+"</p>");
        out.println("<p>There are "+gs.getAliveMutants().size()+" mutants alive </p>");

        if (gs.isTurn(ATTACKER)) {

            InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/resources/Book.java");
            
            out.println("<form action=\"/gammut/attacker\" method=\"post\">");
            out.println("<input type=\"hidden\" name=\"user\" value=\"0\">");
            out.println("<textarea name=\"mutant\" cols=\"100\" rows=\"50\">");
            String line;
            BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
            while((line = is.readLine()) != null)
                out.println(line);
            out.println("</textarea>");
            out.println("<br><input type=\"submit\" value=\"Attack!\">");
            out.println("</form>");

        }

        else {

            response.setIntHeader("Refresh", 5);
            out.println("<h1>Waiting for Defender to take their turn</h1>");

        }

        out.println("</body>");
        out.println("</html>");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Get the text submitted by the user.
        String mutantText = request.getParameter("mutant");

        // If it can be written to file and compiled, end turn. Otherwise, dont.
        if (createMutant(mutantText, "Book")) {
            gs.endTurn();
        }        

        doGet(request, response);
    }

    // Writes text as a Mutant to the appropriate place in the file system.
    private boolean createMutant(String mutantText, String name) throws IOException {

        String original = "";
        String line;

        System.out.println(mutantText);

        InputStream resourceContent = getServletContext().getResourceAsStream("/WEB-INF/resources/Book.java");
        BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
        while((line = is.readLine()) != null) {original += line + "\n";}

        System.out.println(original);

        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> changes = dmp.diff_main(original.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);

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
        for (diff_match_patch.Diff d : newMutant.getDifferences()) {
            diffLog += d.toString();
        }

        if (mt.compileMutant(newMutant)) {gs.addMutant(newMutant); return true;}
        else {folder.delete(); return false;}
    }
}