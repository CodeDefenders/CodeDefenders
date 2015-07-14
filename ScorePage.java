import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ScorePage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    GameState gs;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");

        if (gs.getScore(ATTACKER) > gs.getScore(DEFENDER)) {request.setAttribute("result", 0);}
        else if (gs.getScore(ATTACKER) < gs.getScore(DEFENDER)) {request.setAttribute("result", 1);}
        else {request.setAttribute("result", 2);}

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/score_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}