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

/**
 * Background job to send email verification codes to users
 * @author Steve Jones
 *
 */
public abstract class SendCodeJob extends Job {

  /**
   * The key for the email address in the job parameters
   */
  public static final String EMAIL_KEY = "emailAddress";

  /**
   * Job object constructor
   * @param resourceManager The application's resource manager
   * @param config The application configuration
   * @param id The job ID
   * @param params The job parameters
   * @throws MissingParamException If any required parameters are missing
   * @throws InvalidJobParametersException If the job parameters are invalid
   */
  public SendCodeJob(ResourceManager resourceManager, Properties config, long id, Map<String, String> params) throws MissingParamException, InvalidJobParametersException {
    super(resourceManager, config, id, params);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    StringBuffer emailText = new StringBuffer(getEmailText());
    emailText.append("\n\n");
    emailText.append(buildLink(config));
    emailText.append("\n");

    try {
      EmailSender.sendEmail(config, parameters.get(EMAIL_KEY), getSubject(), emailText.toString());
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
      } else if (null == getCode(dbUser)) {
        throw new InvalidJobParametersException(getCodeDescription() + " not set");
      }
    } catch (Exception e) {
      throw new InvalidJobParametersException("Unhandled exception while validating parameters", e);
    }
  }

  /**
   * Construct the email verification link that the user will click in the email
   * @param config The application configuration
   * @return The link
   * @throws JobFailedException If the link cannot be built
   */
  private String buildLink(Properties config) throws JobFailedException {

    String emailAddress = parameters.get(EMAIL_KEY);

    StringBuffer link = new StringBuffer();

    User dbUser;
    try {
      dbUser = UserDB.getUser(dataSource, emailAddress);
      if (null == dbUser) {
        throw new JobFailedException(id, "User doesn't exist");
      } else {
        String code = getCode(dbUser);
        if (null == code) {
          throw new JobFailedException(id, "The user doesn't have an email verification code");
        } else {
          link.append(config.getProperty("app.urlstub"));
          link.append(getUrlPath());
          link.append('?');
          link.append(VerifyEmailBean.USER_PARAM);
          link.append('=');
          link.append(emailAddress);
          link.append('&');
          link.append(VerifyEmailBean.CODE_PARAM);
          link.append('=');
          link.append(code);
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

  /**
   * Get the URL path
   * @return The URL path
   */
  protected abstract String getUrlPath();

  /**
   * Get the email subject
   * @return The email subject
   */
  protected abstract String getSubject();

  /**
   * Get the email text
   * @return The email text
   */
  protected abstract String getEmailText();

  /**
   * Get the description of the code being used
   * @return The code description
   */
  protected abstract String getCodeDescription();

  /**
   * Get the code
   * @return The code
   */
  protected abstract String getCode(User user);
}
