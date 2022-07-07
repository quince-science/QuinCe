package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

@SuppressWarnings("serial")
public class PositionParseException extends PositionException {

  public PositionParseException(String value) {
    super("Invalid position value " + value);
  }

}
