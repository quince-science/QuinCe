package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

/**
 * Exception for errors encountered during data reduction calculations
 * @author Steve Jones
 *
 */
public class DataReductionException extends Exception {

  /**
   * Serial version UID
   */
  private static final long serialVersionUID = -7961027072943299664L;

  public DataReductionException(String message) {
    super(message);
  }

  public DataReductionException(String message, Throwable cause) {
    super(message, cause);
  }
}
