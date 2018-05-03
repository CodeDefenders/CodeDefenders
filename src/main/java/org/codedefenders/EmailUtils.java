package org.codedefenders;

import org.codedefenders.util.AdminDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Utility class for sending emails. Email credentials are stored in
 * the {@link AdminSystemSettings}. If no email credentials are specified
 * no mails will be sent.
 * <p>
 * Consists of static methods, which allow sending mails.
 */
public class EmailUtils {
	private static final Logger logger = LoggerFactory.getLogger(EmailUtils.class);

	/**
	 * Sends an email (with given subject and given content) to a given recipient.
	 *
	 * @param to      The recipient of the mail.
	 * @param subject The subject of the mail.
	 * @param text    The content of the mail.
	 * @return {@code true} if successful, {@code false} otherwise.
	 */
	public static boolean sendEmail(String to, String subject, String text) {
		final String emailAddress = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS).getStringValue();
		return sendEmail(to, subject, text, emailAddress);
	}

	/**
	 * Sends an email (with given subject and given content) to the system email address.
	 *
	 * @param subject The subject of the mail.
	 * @param text    The content of the mail.
	 * @param replyTo The {@code reply-to} email header.
	 * @return {@code true} if successful, {@code false} otherwise.
	 */
	public static boolean sendEmailToSelf(String subject, String text, String replyTo) {
		final String emailAddress = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAIL_ADDRESS).getStringValue();
		return sendEmail(emailAddress, subject, text, replyTo);
	}

	/**
	 * Sends an email to a given recipient for a given subject and content
	 * with a {@code reply-to} header.
	 *
	 * @param to      The recipient of the mail ({@code to} header).
	 * @param subject The subject of the mail.
	 * @param text    The content of the mail.
	 * @param replyTo The {@code reply-to} email header.
	 * @return {@code true} if successful, {@code false} otherwise.
	 */
	private static boolean sendEmail(String to, String subject, String text, String replyTo) {
		final boolean emailEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue();
		if (!emailEnabled) {
			logger.error("Tried to send a mail, but sending emails is disabled. Update your system settings.");
			return false;
		}

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

			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailAddress, emailPassword);
				}});

			session.setDebug(debug);
			MimeMessage msg = new MimeMessage(session);

			msg.setFrom(new InternetAddress(emailAddress));
			msg.setReplyTo(InternetAddress.parse(replyTo));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			msg.setSubject(subject);
			msg.setContent(text, "text/plain");
			msg.setSentDate(new Date());

			Transport transport = session.getTransport("smtp");
			transport.connect(smtpHost, smtpPort, emailAddress, emailPassword);
			Transport.send(msg);
			transport.close();

			logger.info(String.format("Mail sent: to: %s, replyTo: %s", to, replyTo));
		} catch (MessagingException messagingException) {
			logger.warn("Failed to send email.", messagingException);
			return false;
		}
		return true;
    }
}
