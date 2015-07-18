package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class CreatePage extends HttpServlet {

    protected GameState gs;
    protected MutationTester mt;

    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);
        gs = (GameState) getServletContext().getAttribute("gammut.gamestate");
        mt = (MutationTester) getServletContext().getAttribute("gammut.mutationtester");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/create_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {


    }
}