package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

/**
 * Exception for attempting to add an {@link FileDefinition} where a file with
 * the same description has already been added.
 */
public class InstrumentFileExistsException extends Exception {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 3317044919826150286L;

  /**
   * Basic constructor
   *
   * @param file
   *          The duplicate instrument file
   */
  public InstrumentFileExistsException(FileDefinition file) {
    super("An instrument file with the description '"
      + file.getFileDescription() + "' already exists");
  }
}
