package uk.ac.exeter.QuinCe.jobs.user;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class SendEmailVerificationMailJob extends Job {

	public SendEmailVerificationMailJob(DataSource dataSource, Properties config, long id, List<String> params) throws MissingParamException, InvalidJobParametersException {
		super(dataSource, config, id, params);
	}
	
	@Override
	protected void execute() throws JobFailedException {
		
		StringBuffer emailText = new StringBuffer();
		emailText.append("Click the link\n\n");
		emailText.append(buildLink());
		emailText.append("\n");
		
		try {
			Email email = new SimpleEmail();

			if (Boolean.valueOf(config.getProperty("email.starttls"))) {
				email.setStartTLSEnabled(true);
			}
			
			if (Boolean.valueOf(config.getProperty("email.ssl"))) {
				email.setSSLOnConnect(true);
			}
			
			email.setHostName(config.getProperty("email.hostname"));
			email.setSmtpPort(Integer.parseInt(config.getProperty("email.port")));
			email.setAuthentication(config.getProperty("email.username"), config.getProperty("email.password"));
			email.setFrom(config.getProperty("email.fromaddress"), config.getProperty("email.fromname"));
			email.setSubject("QuinCe Verify Email");
			email.setMsg(emailText.toString());
			email.addTo(parameters.get(1));
			email.send();
		} catch (EmailException e) {
			throw new JobFailedException(id, e);
		}
	}

	@Override
	/**
	 * Validate the job parameters. In this case, make sure a URL stub and user ID is given.
	 * 
	 * For the moment, we don't verify the URL stub - we just make sure we've got a string.
	 */
	protected void validateParameters() throws InvalidJobParametersException {
		if (parameters.size() != 2) {
			throw new InvalidJobParametersException("Incorrect number of parameters");
		}
		
		if (parameters.get(0).length() == 0) {
			throw new InvalidJobParametersException("URL stub is empty");
		}
		
		User dbUser;
		try {
			dbUser = UserDB.getUser(dataSource, parameters.get(1));
			if (null == dbUser) {
				throw new InvalidJobParametersException("The specified user doesn't exist in the database");
			} else if (null == dbUser.getEmailVerificationCode()) {
				throw new InvalidJobParametersException("The specified user doesn't have an email verification code");
			}
		} catch (Exception e) {
			throw new InvalidJobParametersException("Unhandled exception while validating parameters", e);
		}
	}
	
	private String buildLink() throws JobFailedException {
		StringBuffer link = new StringBuffer();
		link.append(parameters.get(0));
		
		User dbUser;
		try {
			dbUser = UserDB.getUser(dataSource, parameters.get(1));
			if (null == dbUser) {
				throw new JobFailedException(id, "User doesn't exist");
			} else {
				String emailCode = dbUser.getEmailVerificationCode();
				if (null == emailCode) {
					throw new JobFailedException(id, "The user doesn't have an email verification code");
				} else {
					link.append(emailCode);
				}
			}
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		}
		
		return link.toString();
	}
}
