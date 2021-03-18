package org.codedefenders.servlets.registration;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.UserEntity;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.input.CodeDefendersValidator;

@WebServlet(Paths.USER)
public class UserServlet extends HttpServlet {

    @Inject
    private MessagesBean messages;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        CodeDefendersValidator validator = new CodeDefendersValidator();

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String formType = request.getParameter("formType");

        switch (formType) {
            case "create":
                String confirm = request.getParameter("confirm");
                // TODO Move validation logic to validation a validation filter.
                // http://zetcode.com/java/validationfilter/
                if (!(validator.validUsername(username))) {
                    // This check should be performed in the user interface too.
                    messages.add("Could not create user. Invalid username.");
                } else if (!validator.validPassword(password)) {
                    // This check should be performed in the user interface too.
                    messages.add("Could not create user. Invalid password.");
                } else if (!validator.validEmailAddress(email)) {
                    // This check should be performed in the user interface too.
                    messages.add("Could not create user. Invalid E-Mail address.");
                } else if (!password.equals(confirm)) {
                    // This check should be performed in the user interface too.
                    messages.add("Could not create user. Password entries did not match.");
                } else if (UserDAO.getUserByName(username) != null) {
                    messages.add("Could not create user. Username is already taken.");
                } else if (UserDAO.getUserByEmail(email) != null) {
                    messages.add("Could not create user. Email has already been used. You can reset your password.");
                } else {
                    UserEntity newUser = new UserEntity(username, UserEntity.encodePassword(password), email);
                    if (newUser.insert()) {
                        messages.add("Your user has been created. You can login now.");
                    } else {
                        // TODO: How about some error handling?
                        messages.add("Could not create a user for you, sorry!");
                    }
                }
                RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
                dispatcher.forward(request, response);
                break;

            default:
                // Anything different than "create" is an error, so we not allow it
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                break;
        }

    }

}
