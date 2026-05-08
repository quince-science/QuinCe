package uk.ac.exeter.QuinCe.data.Files;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;

/**
 * Exception class for handling attempts to store raw data files that already
 * exist
 */
@SuppressWarnings("serial")
public class FileExistsException extends Exception {

  /**
   * The exception message, generated in the constructor
   */
  private String message;

  /**
   * The exception constructor
   *
   * @param dataSource
   *          A data source
   * @param instrumentID
   *          The database ID of the instrument for the file being stored
   * @param fileName
   *          The file name
   */
  public FileExistsException(DataSource dataSource, long instrumentID,
    String fileName) {
    super();
    try {
      String instrumentName = InstrumentDB
        .getInstrument(dataSource, instrumentID).getName();
      message = "File '" + fileName + "' already exists for instrument '"
        + instrumentName + "'";
    } catch (Exception e) {
      message = "File '" + fileName + "' already exists for this instrument";
    }
  }

  /**
   * Constructor for an existing file found that overlaps the specified date
   * range
   *
   * @param fileDescription
   *          The file description
   * @param startDate
   *          The range start
   * @param endDate
   *          The range end
   */
  public FileExistsException(String fileDescription) {
    super();
    message = "A " + fileDescription
      + " file already exists that overlaps this file";
  }

  @Override
  public String getMessage() {
    return message;
  }

}
