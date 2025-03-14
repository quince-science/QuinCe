package uk.ac.exeter.QuinCe.utils;

/**
 * Miscellaneous utilities for handling exceptions.
 *
 * <p>
 * This class extends {@link org.apache.commons.lang3.exception.ExceptionUtils}
 * so methods from that class can be called directly through this one, thereby
 * reducing issues with {@code import} statements if a class needs to use
 * methods from both classes.
 * </p>
 */
public class ExceptionUtils
  extends org.apache.commons.lang3.exception.ExceptionUtils {

  /**
   * A cut-down stack trace printer that stops the stack trace after the last
   * QuinCe exception in the trace.
   *
   * @param e
   *          The exception
   */
  public static void printStackTrace(Throwable e) {

    StringBuilder output = new StringBuilder();

    output.append(getExceptionString(e));

    if (null != e.getCause()) {
      output.append("Caused by: ");
      output.append(getExceptionString(e.getCause()));
    }

    System.err.print(output);
  }

  private static String getExceptionString(Throwable e) {

    StringBuilder output = new StringBuilder();

    output.append(e.getClass().getCanonicalName());
    output.append(": ");
    output.append(e.getMessage());
    output.append('\n');

    boolean quinceExceptionFound = false;

    for (StackTraceElement element : e.getStackTrace()) {

      // Stop the trace if we've run out of
      if (quinceExceptionFound && !isQuinceElement(element)) {
        output.append("\t...\n");
        break;
      } else {
        output.append('\t');
        output.append(element.toString());
        output.append('\n');

        if (isQuinceElement(element)) {
          quinceExceptionFound = true;
        }
      }
    }

    return output.toString();
  }

  private static boolean isQuinceElement(StackTraceElement element) {
    return element.getClassName().contains("QuinCe");
  }
}
