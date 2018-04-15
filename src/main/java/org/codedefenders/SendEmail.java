package org.codedefenders;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SendEmail extends HttpServlet {

	public void doPost(HttpServletRequest request,
	       HttpServletResponse response) throws ServletException, IOException {

		final String name     = request.getParameter("name");
		final String email    = request.getParameter("email");
		final String subject  = request.getParameter("subject");
		final String message  = request.getParameter("message");
        final String from = String.format("\"%s\"<%s>", name, email);

        if (EmailUtils.sendEmailToSelf(from, subject, message)) {
            request.getSession().setAttribute("emailSent", "Thanks for your message, we'll get back to you soon! --The Code Defenders Team");
        } else {
            request.getSession().setAttribute("emailSent", "Sorry! The message was not sent. Please, try emailing us: {j.rojas,gordon.fraser}@sheffield.ac.uk.");
        }

		response.sendRedirect(request.getContextPath()+"/#contact");
	}
}
