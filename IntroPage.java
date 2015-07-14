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

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/intro_view.jsp");
        dispatcher.forward(request, response);
    }
}