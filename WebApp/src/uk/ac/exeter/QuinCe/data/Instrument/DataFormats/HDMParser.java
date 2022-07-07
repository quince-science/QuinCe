package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public class HDMParser extends PositionParser {

  private boolean parsed = false;

  private String hemisphere;

  private int degrees;

  private double minutes;

  protected HDMParser(HemisphereMultiplier hemisphereMultiplier) {
    super(hemisphereMultiplier);
  }

  @Override
  protected double getNumericValue(String value) throws PositionParseException {
    if (!parsed) {
      parse(value);
    }

    return calculateDecimalDegrees(degrees, minutes);
  }

  @Override
  protected String getHemisphere(String value, String dummy)
    throws PositionParseException {
    if (!parsed) {
      parse(value);
    }

    return hemisphere;
  }

  private void parse(String value) throws PositionParseException {

    // Split on whitespace
    String[] split = value.split("\\s+");

    if (split.length != 3) {
      throw new PositionParseException("Malformed position value " + value);
    }

    hemisphere = split[0];
    degrees = Integer.parseInt(split[1]);
    minutes = Double.parseDouble(split[2]);

    parsed = true;
  }
}
