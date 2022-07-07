package uk.ac.exeter.QuinCe.utils;

/**
 * Exception for errors in method parameters
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class ParameterException extends RuntimeException {

  /**
   * Constructor
   *
   * @param parameterName
   *          The name of the parameter for which the exception is being raised
   * @param reason
   *          The reason for the exception
   */
  public ParameterException(String parameterName, String reason) {
    super(parameterName + ' ' + reason);
  }

}
