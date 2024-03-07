package uk.ac.exeter.QuinCe.web;

/**
 * Exception class for miscellaneous errors in beans
 */
@SuppressWarnings("serial")
public class BeanException extends Exception {

  public BeanException(String message) {
    super(message);
  }

  public BeanException(String message, Throwable cause) {
    super(message, cause);
  }

}
