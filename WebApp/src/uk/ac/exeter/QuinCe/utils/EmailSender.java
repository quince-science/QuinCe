package uk.ac.exeter.QuinCe.utils;

import java.util.Properties;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/**
 * Utility class for sending emails
 * 
 * @author Steve Jones
 *
 */
public class EmailSender {

  /**
   * Send an email. The email server/authentication details are extracted from
   * the supplied configuration.
   *
   * <p>
   * The following configuration values are used:
   * </p>
   * <ul>
   * <li>{@code email.starttls} Indicates whether or not STARTTLS should be used
   * ({@code true/false}). If not present, {@code false} is assumed.</li>
   * <li>{@code email.ssl} Indicates whether or not SSL should be used
   * ({@code true/false}). If not present, {@code false} is assumed.</li>
   * <li>{@code email.host} The email server.</li>
   * <li>{@code email.port} The port on the email server.</li>
   * <li>{@code email.username} The username for authentication on the email
   * server. If this is not present (or empty), authentication will not be
   * attempted.</li>
   * <li>{@code email.password} The password for authentication on the email
   * server (if authentication is being used).</li>
   * <li>{@code email.fromaddress} The From address for emails sent by the
   * application.</li>
   * <li>{@code email.fromname} The From name for emails sent by the
   * application.</li>
   * </ul>
   *
   *
   * @param config
   *          The application configuration
   * @param address
   *          The destination email address
   * @param subject
   *          The email subject
   * @param message
   *          The email contents
   * @throws EmailException
   *           If the email could not be sent
   */
  public static void sendEmail(Properties config, String address,
    String subject, String message) throws EmailException {
    Email email = new SimpleEmail();

    email.setStartTLSEnabled(
      Boolean.valueOf(config.getProperty("email.starttls", "false")));
    email.setSSLOnConnect(
      Boolean.valueOf(config.getProperty("email.ssl", "false")));

    email.setHostName(config.getProperty("email.hostname"));
    email.setSmtpPort(Integer.parseInt(config.getProperty("email.port")));

    String userName = config.getProperty("email.username");
    if (null != userName && userName.trim().length() > 0) {
      email.setAuthentication(config.getProperty("email.username"),
        config.getProperty("email.password"));
    }
    email.setFrom(config.getProperty("email.fromaddress"),
      config.getProperty("email.fromname"));
    email.setSubject(subject);
    email.setMsg(message);
    email.addTo(address);
    email.send();
  }
}
