package org.codedefenders.servlets;

import org.codedefenders.util.EmailUtils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SendEmail extends HttpServlet {

	public void doPost(HttpServletRequest request,
	       HttpServletResponse response) throws ServletException, IOException {

		final String name     = request.getParameter("name");
		final String email    = request.getParameter("email");
		final String subject  = request.getParameter("subject");
		final String message  = String.format("From: %s <%s>\n\n%s", name, email, request.getParameter("message"));

        if (EmailUtils.sendEmailToSelf(subject, message, email)) {
            request.getSession().setAttribute("emailSent", "Thanks for your message, we'll get back to you soon! --The Code Defenders Team");
        } else {
            request.getSession().setAttribute("emailSent", "Sorry! There was an error when trying to send the message.");
        }

		response.sendRedirect(request.getContextPath()+"/contact");
	}
}
