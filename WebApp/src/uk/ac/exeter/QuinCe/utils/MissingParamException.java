package uk.ac.exeter.QuinCe.utils;

/**
 * Exception thrown when a {@link MissingParam} check fails.
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class MissingParamException extends ParameterException {

  /**
   * Basic constructor
   *
   * @param varName
   *          The parameter that failed the test
   */
  public MissingParamException(String varName) {
    super(varName, "parameter is missing or empty");
  }

  /**
   * Missing parameter with a specific message
   *
   * @param varName
   *          Parameter name
   * @param message
   *          Message
   */
  public MissingParamException(String varName, String message) {
    super(varName, message);
  }
}
