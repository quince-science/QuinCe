package uk.ac.exeter.QuinCe.data.Instrument;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;

/**
 * Exception for missing run types in files.
 */
@SuppressWarnings("serial")
public class MissingRunTypeException extends FileDefinitionException {

  private RunTypeAssignment runType;

  public RunTypeAssignment getRunType() {
    return runType;
  }

  /**
   * Simple error
   *
   * @param message
   *          The error message
   */
  public MissingRunTypeException(String message, String runType) {
    super(message);
    this.runType = new RunTypeAssignment(runType, RunTypeCategory.IGNORED);
  }
}
