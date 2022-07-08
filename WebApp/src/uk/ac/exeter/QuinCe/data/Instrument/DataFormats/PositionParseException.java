package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

@SuppressWarnings("serial")
public class PositionParseException extends PositionException {

  public PositionParseException(String value) {
    super("Invalid position value " + value);
  }

  public PositionParseException(double value) {
    super("Invalid position value " + value);
  }

  public PositionParseException(String item, String value) {
    super("Invalid " + item + " value " + value);
  }

  public PositionParseException(String item, double value) {
    super("Invalid " + item + " value " + value);
  }

}
