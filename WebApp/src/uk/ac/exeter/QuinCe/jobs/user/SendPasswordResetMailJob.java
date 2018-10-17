package uk.ac.exeter.QuinCe.jobs.user;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.User.ResetPasswordBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Background job to send email verification codes to users
 * @author Steve Jones
 *
 */
public class SendPasswordResetMailJob extends SendCodeJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Lost Password Code";

  /**
   * Job object constructor
   * @param resourceManager The application's resource manager
   * @param config The application configuration
   * @param id The job ID
   * @param params The job parameters
   * @throws MissingParamException If any required parameters are missing
   * @throws InvalidJobParametersException If the job parameters are invalid
   */
  public SendPasswordResetMailJob(ResourceManager resourceManager, Properties config, long id, Map<String, String> params) throws MissingParamException, InvalidJobParametersException {
    super(resourceManager, config, id, params);
  }

  @Override
  public String getJobName() {
    return jobName;
  }

  @Override
  protected String getSubject() {
    return "Reset QuinCe password";
  }

  @Override
  protected String getEmailText() {
    return "Click the link below to reset your QuinCe password. If you did not request this link, you can safely ignore it.";
  }

  @Override
  protected String getCodeDescription() {
    return "Password reset code";
  }

  @Override
  protected String getCode(User user) {
    return user.getPasswordResetCode();
  }

  @Override
  protected String getUrlPath() {
    return ResetPasswordBean.PATH;
  }
}
