package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Exception class for unrecognised hemisphere values in positions
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
    super("hemisphere", hemisphere);
  }

}
