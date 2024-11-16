package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public abstract class HDMParser extends PositionParser {

  protected int degrees;

  protected double minutes;

  protected HDMParser() {
    super();
  }

  @Override
  protected double getNumericValue(String value) throws PositionParseException {
    parse(value);

    return calculateDecimalDegrees(degrees, minutes);
  }

  protected abstract void parse(String value) throws PositionParseException;
}
