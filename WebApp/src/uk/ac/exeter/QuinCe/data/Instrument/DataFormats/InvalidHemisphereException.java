package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception class for unrecognised hemisphere values in positions
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class InvalidHemisphereException extends PositionParseException {

  /**
   * Simple constructor
   *
   * @param hemisphere
   *          The invalid hemisphere value
   */
  public InvalidHemisphereException(String hemisphere) {
    super("The hemisphere value '" + hemisphere + "' is invalid");
  }

}
