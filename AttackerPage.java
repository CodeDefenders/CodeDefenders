import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.lang.reflect.*;
import java.util.Scanner;
import java.net.*;

public class AttackerPage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        GameState gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Attacker Window</title>");
        out.println("</head>");
        out.println("<body>");

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

        // Open GameState for storing new data.
        GameState gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        // Get the text submitted by the user.
        String mutantText = request.getParameter("mutant");
        // Write it to a Java File.
        Mutant newMutant = writeMutantToJava(mutantText, "Book");
        // Add a record of the mutant to the Game State.
        gs.addMutant(newMutant);
        // Set the GameState.
        getServletContext().setAttribute("gammut.gamestate", gs);
        gs.endTurn();

        // Display as if get request received.
        doGet(request, response);
    }

    // Writes text as a Mutant to the appropriate place in the file system.
    private Mutant writeMutantToJava(String mutantText, String name) throws IOException {

        Long timestamp = System.currentTimeMillis();
        File folder = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+timestamp));
        folder.mkdir();

        File mutant = new File(getServletContext().getRealPath("/WEB-INF/mutants/"+timestamp+"/"+name+".java"));
        FileWriter fw = new FileWriter(mutant);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(mutantText);
        bw.close();
        return new Mutant(folder, name);
    }
}