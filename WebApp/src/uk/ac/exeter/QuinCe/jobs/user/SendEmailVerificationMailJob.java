package uk.ac.exeter.QuinCe.jobs.user;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.User.VerifyEmailBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Background job to send email verification codes to users
 * @author Steve Jones
 *
 */
public class SendEmailVerificationMailJob extends SendCodeJob {
  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Email Verification";

  /**
   * Job object constructor
   * @param resourceManager The application's resource manager
   * @param config The application configuration
   * @param id The job ID
   * @param params The job parameters
   * @throws MissingParamException If any required parameters are missing
   * @throws InvalidJobParametersException If the job parameters are invalid
   */
  public SendEmailVerificationMailJob(ResourceManager resourceManager, Properties config, long id, Map<String, String> params) throws MissingParamException, InvalidJobParametersException {
    super(resourceManager, config, id, params);
  }

  @Override
  public String getJobName() {
    return jobName;
  }

  @Override
  protected String getSubject() {
    return "Activate your QuinCe account";
  }

  @Override
  protected String getEmailText() {
    return "Your QuinCe account has been created. Click the link below to activate it.";
  }

  @Override
  protected String getCodeDescription() {
    return "Email verification code";
  }

  @Override
  protected String getCode(User user) {
    return user.getEmailVerificationCode();
  }

  @Override
  protected String getUrlPath() {
    return VerifyEmailBean.PATH;
  }
}
