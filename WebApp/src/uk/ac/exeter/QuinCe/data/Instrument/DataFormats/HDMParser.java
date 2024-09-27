package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public abstract class HDMParser extends PositionParser {

  protected boolean parsed = false;

  protected int degrees;

  protected double minutes;

  protected HDMParser() {
    super();
  }

  @Override
  protected double getNumericValue(String value) throws PositionParseException {
    if (!parsed) {
      parse(value);
    }

    return calculateDecimalDegrees(degrees, minutes);
  }

  protected void parse(String value) throws PositionParseException {
    parseAction(value);
    parsed = true;
  }

  protected abstract void parseAction(String value)
    throws PositionParseException;
}
