package uk.ac.exeter.QuinCe.utils;

import java.util.Properties;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/**
 * Utility class for sending emails
 * @author Steve Jones
 *
 */
public class EmailSender {

	/**
	 * Send an email. The email server/authentication details are extracted from the supplied config
	 * @param config The application configuration
	 * @param address The destination email address
	 * @param subject The email subject
	 * @param message The email contents
	 * @throws EmailException If the email could not be sent
	 */
	public static void sendEmail(Properties config, String address, String subject, String message) throws EmailException {
		Email email = new SimpleEmail();

		email.setStartTLSEnabled(Boolean.valueOf(config.getProperty("email.starttls", "false")));
		email.setSSLOnConnect(Boolean.valueOf(config.getProperty("email.ssl", "false")));
		
		email.setHostName(config.getProperty("email.hostname"));
		email.setSmtpPort(Integer.parseInt(config.getProperty("email.port")));
		
		String userName = config.getProperty("email.username");
		if (null != userName && userName.trim().length() > 0) {
			email.setAuthentication(config.getProperty("email.username"), config.getProperty("email.password"));
		}
		email.setFrom(config.getProperty("email.fromaddress"), config.getProperty("email.fromname"));
		email.setSubject(subject);
		email.setMsg(message);
		email.addTo(address);
		email.send();
	}
}
