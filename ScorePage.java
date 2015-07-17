package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ScorePage extends HttpServlet {

    public static final int ATTACKER = 0;
    public static final int DEFENDER = 1;

    protected GameState gs;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/score_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}