package uk.ac.exeter.QuinCe.utils;

public class ExceptionUtils {

  /**
   * A cut-down stack trace printer that stops the stack trace after the last
   * QuinCe exception in the trace.
   *
   * @param e
   *          The exception
   */
  public static void printStackTrace(Throwable e) {

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

    System.err.print(output);
  }

  private static boolean isQuinceElement(StackTraceElement element) {
    return element.getClassName().contains("QuinCe");
  }

  /**
   * Get an exception's stack trace as a string.
   *
   * <p>
   * This is a simple passthrough to
   * {@link org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace} for
   * code simplicity.
   * </p>
   *
   * @param e
   *          The exception
   * @return The stack trace
   */
  public static String getStackTrace(Exception e) {
    return org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);
  }
}
