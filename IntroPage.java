import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class IntroPage extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        GameState gs = (GameState)getServletContext().getAttribute("gammut.gamestate");
        if (gs == null) {getServletContext().setAttribute("gammut.gamestate", new GameState());}

        MutationTester mt = (MutationTester)getServletContext().getAttribute("gammut.mutationtester");
        if (mt == null) {getServletContext().setAttribute("gammut.mutationtester", new MutationTester("Book"));}

        try {
            Class gameClass = Class.forName("Book");
            getServletContext().setAttribute("gammut.gameclass", gameClass);
        }
        catch (ClassNotFoundException e) {}

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Intro Window</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>This is the intro window!</h1>");
        out.println("<button><a href=\"/gammut/attacker\">Attacker</a></button>");
        out.println("<button><a href=\"/gammut/defender\">Defender</a></button>");
        out.println("</body>");
        out.println("</html>");
    }
}