package uk.ac.exeter.QuinCe.utils;

/**
 * Exception thrown when a {@link MissingParam} check fails.
 * @author Steve Jones
 *
 */
public class MissingParamException extends ParameterException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -5143400042710795233L;

  /**
   * Basic constructor
   * @param varName The parameter that failed the test
   */
  public MissingParamException(String varName) {
    super(varName, "parameter is null");
  }

  /**
   * Missing parameter with a specific message
   * @param varName Parameter name
   * @param message Message
   */
  public MissingParamException(String varName, String message) {
    super(varName, message);
  }
}
