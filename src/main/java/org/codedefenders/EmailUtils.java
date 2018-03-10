package org.codedefenders;

import org.codedefenders.util.AdminDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * Created by thomas on 20/01/2017.
 */
public class EmailUtils {

	private static final Logger logger = LoggerFactory.getLogger(AdminDAO.class);

	public static boolean sendEmail(String to, String subject, String text) {

		final String smtpHost = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_SMTP_HOST).getStringValue();
		final int smtpPort = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_SMTP_PORT).getIntValue();
		final String emailAddress = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS).getStringValue();
		final String emailPassword = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_PASSWORD).getStringValue();
		final boolean debug = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.DEBUG_MODE).getBoolValue();


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

			session.setDebug(debug);
			MimeMessage msg = new MimeMessage(session);

			msg.setFrom(new InternetAddress(emailAddress));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			msg.setSubject(subject);
			msg.setContent(text, "text/plain");
			msg.setSentDate(new Date());

			Transport transport = session.getTransport("smtp");
			transport.connect(smtpHost, smtpPort, emailAddress, emailPassword);
			Transport.send(msg);
			transport.close();
		} catch (MessagingException messagingException) {
			logger.warn(messagingException.toString());
			return false;
		}
		return true;
    }

}
