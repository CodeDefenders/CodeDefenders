import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ScorePage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    GameState gs;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Final Scores</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>SCORE WINDOW</h1>");
        if (gs.getScore(ATTACKER) > gs.getScore(DEFENDER)) {
            out.println("<p>Attacker has won!</p>");
        }
        else if (gs.getScore(ATTACKER) < gs.getScore(DEFENDER)) {
            out.println("<p>Defender has won!</p>");
        }
        else {
            out.println("<p>It was a tie!</p>");
        }
        out.println("</body>");
        out.println("</html>");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}