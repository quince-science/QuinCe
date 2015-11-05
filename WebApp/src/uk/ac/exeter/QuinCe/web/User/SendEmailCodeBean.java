package uk.ac.exeter.QuinCe.web.User;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class SendEmailCodeBean extends BaseManagedBean {
	
	public SendEmailCodeBean() {
		// Do Nothing
	}

	private String email = null;
	
	/**
	 * Sends an email verification code to a user (based on email address)
	 * If the email address is not supplied or invalid, simply returns to the login
	 * screen.
	 */
	public String sendCode() {
		
		String result = VALIDATION_FAILED_RESULT;
		
		if (null != email && email.length() > 0) {
			try {
				User user = UserDB.getUser(ServletUtils.getDBDataSource(), email);
				if (null != user) {
					UserDB.generateEmailVerificationCode(ServletUtils.getDBDataSource(), user);
					List<String> emailJobParams = new ArrayList<String>();
					emailJobParams.add(email);
					
					JobManager.addInstantJob(ServletUtils.getDBDataSource(), ServletUtils.getAppConfig(), user, "uk.ac.exeter.QuinCe.jobs.user.SendEmailVerificationMailJob", emailJobParams);
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
	
	public void setEmail(String email) {
		this.email = email;
	}
}
