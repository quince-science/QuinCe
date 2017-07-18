package uk.ac.exeter.QuinCe.web.User;

import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.user.SendEmailVerificationMailJob;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Managed bean for sending email verification codes to users
 * @author Steve Jones
 *
 */
public class SendEmailCodeBean extends BaseManagedBean {
	
	/**
	 * Basic constructor
	 */
	public SendEmailCodeBean() {
		// Do Nothing
	}

	/**
	 * The email address to which the verification code should be sent
	 */
	private String email = null;
	
	/**
	 * Sends an email verification code to a user (based on email address)
	 * If the email address is not supplied or invalid, simply returns to the login
	 * screen.
	 * @return The routing result for the web application
	 */
	public String sendCode() {
		
		String result = VALIDATION_FAILED_RESULT;
		
		if (null != email && email.length() > 0) {
			try {
				User user = UserDB.getUser(ServletUtils.getDBDataSource(), email);
				if (null != user) {
					UserDB.generateEmailVerificationCode(ServletUtils.getDBDataSource(), user);
					Map<String, String> emailJobParams = new HashMap<String, String>(1);
					emailJobParams.put(SendEmailVerificationMailJob.EMAIL_KEY, email);
					
					JobManager.addInstantJob(ServletUtils.getResourceManager(), ServletUtils.getAppConfig(), user, "uk.ac.exeter.QuinCe.jobs.user.SendEmailVerificationMailJob", emailJobParams);
					result = SUCCESS_RESULT;
				}
			} catch (Exception e) {
				result = internalError(e);
			}
		}
		
		return result;
	}
	
	/**
	 * Get the email address. If it's not already been set in the
	 * bean, get it from the request
	 * @return The email address
	 */
	public String getEmail() {
		String result = email;
		
		if (null == email || email.length() == 0) {
			result = getRequestParameter(VerifyEmailBean.USER_PARAM);
		}
		
		
		return result;
	}
	
	/**
	 * Set the email address
	 * @param email The email address
	 */
	public void setEmail(String email) {
		this.email = email;
	}
}
