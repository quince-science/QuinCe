package uk.ac.exeter.QuinCe.jobs.user;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.mail.EmailException;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.EmailSender;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.User.VerifyEmailBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class SendEmailVerificationMailJob extends Job {

	public static final String EMAIL_KEY = "emailAddress";
	
	public SendEmailVerificationMailJob(ResourceManager resourceManager, Properties config, long id, Map<String, String> params) throws MissingParamException, InvalidJobParametersException {
		super(resourceManager, config, id, params);
	}
	
	@Override
	protected void execute(JobThread thread) throws JobFailedException {
		
		StringBuffer emailText = new StringBuffer();
		emailText.append("Click the link\n\n");
		emailText.append(buildLink(config));
		emailText.append("\n");
		
		try {
			EmailSender.sendEmail(config, parameters.get(EMAIL_KEY), "Activate your QuinCe account", emailText.toString());
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
		if (parameters.size() != 1) {
			throw new InvalidJobParametersException("Incorrect number of parameters");
		}
		
		User dbUser;
		try {
			dbUser = UserDB.getUser(dataSource, parameters.get(EMAIL_KEY));
			if (null == dbUser) {
				throw new InvalidJobParametersException("The specified user doesn't exist in the database");
			} else if (null == dbUser.getEmailVerificationCode()) {
				throw new InvalidJobParametersException("The specified user doesn't have an email verification code");
			}
		} catch (Exception e) {
			throw new InvalidJobParametersException("Unhandled exception while validating parameters", e);
		}
	}
	
	private String buildLink(Properties config) throws JobFailedException {
		
		String emailAddress = parameters.get(EMAIL_KEY);
		
		StringBuffer link = new StringBuffer();

		User dbUser;
		try {
			dbUser = UserDB.getUser(dataSource, emailAddress);
			if (null == dbUser) {
				throw new JobFailedException(id, "User doesn't exist");
			} else {
				String emailCode = dbUser.getEmailVerificationCode();
				if (null == emailCode) {
					throw new JobFailedException(id, "The user doesn't have an email verification code");
				} else {
					link.append(config.getProperty("app.urlstub"));
					link.append(VerifyEmailBean.PATH);
					link.append('?');
					link.append(VerifyEmailBean.USER_PARAM);
					link.append('=');
					link.append(emailAddress);
					link.append('&');
					link.append(VerifyEmailBean.CODE_PARAM);
					link.append('=');
					link.append(emailCode);
				}
			}
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		}
		
		return link.toString();
	}
	
	@Override
	protected String getFinishState() {
		// Since we ignore interrupts, we always return FINISHED
		return FINISHED_STATUS;
	}
}
