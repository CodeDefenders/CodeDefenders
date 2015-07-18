package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class IntroPage extends HttpServlet {

    GameState gs;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        getServletContext().setAttribute("gammut.gamestate", new GameState());
        gs = (GameState)getServletContext().getAttribute("gammut.gamestate");

        gs.setClassName("Book");

        getServletContext().setAttribute("gammut.mutationtester", new MutationTester());
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/intro_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        gs.setClassName((String)request.getParameter("sourcecode"));

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/intro_view.jsp");
        dispatcher.forward(request, response);
    }
}