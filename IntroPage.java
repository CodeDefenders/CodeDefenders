package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class IntroPage extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        getServletContext().setAttribute("gammut.gamestate", new GameState());

        try {
            Class gameClass = Class.forName("Book");
            getServletContext().setAttribute("gammut.gameclass", gameClass);
        }
        catch (ClassNotFoundException e) {}

        getServletContext().setAttribute("gammut.mutationtester", new MutationTester("Book"));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/intro_view.jsp");
        dispatcher.forward(request, response);
    }
}