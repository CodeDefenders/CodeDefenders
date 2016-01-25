package org.codedefenders;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class SendEmail extends HttpServlet {

	public void doPost(HttpServletRequest request,
	       HttpServletResponse response) throws ServletException, IOException {

		ServletContext context = getServletContext();
		final String smtpHost = context.getInitParameter("smtpHost");
		final String smtpPort = context.getInitParameter("smtpPort");
		final String emailAddress = context.getInitParameter("emailAddress");
		final String emailPassword = context.getInitParameter("emailPassword");

		final String name     = request.getParameter("name");
		final String email    = request.getParameter("email");
		final String subject  = request.getParameter("subject");
		final String message  = request.getParameter("message");

		try	{
			Properties props = System.getProperties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.host", smtpHost);
			props.put("mail.smtp.port", smtpPort);

			Session session = Session.getInstance(props, new javax.mail.Authenticator(){
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailAddress, emailPassword);
				}});

			MimeMessage msg = new MimeMessage(session);

			msg.setFrom(new InternetAddress(String.format("\"%s\"<%s>", name, email)));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
			msg.setReplyTo(InternetAddress.parse(String.format("\"%s\"<%s>", name, email)));
			msg.setSubject(subject);
			msg.setContent(message, "text/plain");
			msg.setSentDate(new Date());

			Transport transport = session.getTransport("smtp");
			transport.connect(smtpHost, Integer.parseInt(smtpPort), emailAddress, emailPassword);
			transport.send(msg);
			transport.close();

			request.getSession().setAttribute("emailSent", "Email Sent Successfully");
		} catch (MessagingException messagingException) {
			System.out.print(messagingException);
			request.getSession().setAttribute("emailSent", "Error Sending Email.");
		}
		response.sendRedirect("contact");
	}
}