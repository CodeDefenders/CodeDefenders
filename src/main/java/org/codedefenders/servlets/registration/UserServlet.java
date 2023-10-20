package org.codedefenders.servlets.registration;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;


@WebServlet(Paths.USER)
public class UserServlet extends HttpServlet {

    @Inject
    private MessagesBean messages;

    @Inject
    private UserService userService;

    @Inject
    private URLUtils url;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("create".equals(request.getParameter("formType"))) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String confirm = request.getParameter("confirm");
            String email = request.getParameter("email");

            if (!password.equals(confirm)) {
                // This check should be performed in the user interface too.
                messages.add("Could not create user. Password entries did not match.");
            } else {

                Optional<String> result = userService.registerUser(username, password, email);
                if (result.isEmpty()) {
                    messages.add("Your user has been created. You can login now.");
                } else {
                    messages.add(result.get());
                }
            }
            response.sendRedirect(url.forPath(Paths.LOGIN));
        } else { // Anything different from "create" is an error, so we not allow it
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
