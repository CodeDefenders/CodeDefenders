package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;

public class LoginManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        String username = (String)request.getParameter("username");
        String password = (String)request.getParameter("password");
        String formType = (String)request.getParameter("formType");
        int uid;

        if (formType.equals("create")) {
            String confirm = (String)request.getParameter("confirm");
            System.out.println("Try to create");
            if ((password.equals(confirm))&&(DatabaseAccess.getUserForName(username) == null)) {
                User newUser = new User(username, password);
                newUser.insert();

                HttpSession session = request.getSession();
                session.setAttribute("uid", newUser.id);
                session.setAttribute("username", newUser.username);
                session.setMaxInactiveInterval(1200);

                response.sendRedirect("games");
            }
            else {
                request.setAttribute("isError", true);
                RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
                dispatcher.forward(request, response);
            }
        }

        else if (formType.equals("login")) {
            User activeUser = DatabaseAccess.getUserForName(username);

            if ((activeUser != null)&&(activeUser.password.equals(password))) {
                HttpSession session = request.getSession();
                session.setAttribute("uid", activeUser.id);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(1200);

                response.sendRedirect("games");
            }
            else {
                RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
                dispatcher.forward(request, response);
            }
        }

        else if (formType.equals("logOut")) {

            HttpSession session = request.getSession();
            session.invalidate();

            RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
            dispatcher.forward(request, response);
        }

        else {
            RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
            dispatcher.forward(request, response);
        }
    }
}