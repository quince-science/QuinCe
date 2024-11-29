package uk.ac.exeter.QuinCe.data.Dataset.QC;

import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;

/**
 * This interface specifies the minimum set of methods required for a any type
 * of QC routine in QuinCe.
 * 
 * <p>
 * Each {@code Routine} consists of a human-readable name, and messages that are
 * applied to QC flags when they are set to anything that isn't considered Good
 * (see {@link Flag#isGood()}).
 * </p>
 * 
 * <p>
 * The messages are not necessarily stored in a QC result. For example,
 * {@link AutoQCJob}s will simply store the {@link Flag} and which
 * {@code Routine} triggered that flag. Calls to {@link #getShortMessage()} or
 * {@link #getLongMessage(RoutineFlag)} will then be used to retrieve the QC
 * message.
 * </p>
 */
public interface Routine {

  /**
   * The name of the routine.
   * 
   * @return The routine name.
   */
  public String getName();

  /**
   * Returns short version of the QC message reported by this {@code Routine}.
   * 
   * <p>
   * This is a simple message that describes the type of check the routine
   * performed, without any details of the specific value(s) that caused a
   * {@link RoutineFlag} to be set.
   * </p>
   * 
   * @return The short QC message for the {@code Routine}.
   */
  public String getShortMessage();

  /**
   * Returns the fully detailed version of the QC message reported by this
   * {@code
   * Routine}.
   * 
   * <p>
   * This will include specific details of why a {@link RoutineFlag} was raised
   * by the {@code Routine}, such as the expected value(s) versus the value(s)
   * in the data.
   * </p>
   * 
   * @param flag
   *          The QC flag raised by the {@code Routine}, containing detailed
   *          information of the cause.
   * @return The long form QC message for the {@code Routine}.
   */
  public String getLongMessage(RoutineFlag flag);
}
