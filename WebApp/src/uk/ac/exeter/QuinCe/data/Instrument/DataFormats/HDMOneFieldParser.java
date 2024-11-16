package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public class HDMOneFieldParser extends HDMParser {

  private PositionSpecification posSpec;

  private String hemisphere;

  protected HDMOneFieldParser(PositionSpecification posSpec) {
    super();
    this.posSpec = posSpec;
  }

  @Override
  protected String getHemisphere(String value, String dummy)
    throws PositionParseException {

    parse(value);

    return hemisphere;
  }

  @Override
  protected void parse(String value) throws PositionParseException {

    // Split on whitespace
    String[] split = value.split("\\s+");

    if (split.length != 3) {
      throw new PositionParseException(value);
    }

    hemisphere = split[0];
    if (!posSpec.isHemisphereValid(hemisphere)) {
      throw new PositionParseException(
        "Invalid hemisphere value " + hemisphere);
    }

    degrees = Integer.parseInt(split[1]);
    minutes = Double.parseDouble(split[2]);
  }

  // For this parser, we ignore the passed in hemisphere value (which will be
  // null because there's no separate column) and use our own extracted
  // hemisphere value.
  @Override
  public double parsePosition(String value, String hemisphereIgnored)
    throws PositionParseException {

    double numericValue = getNumericValue(value);

    if (null != this.hemisphere) {
      numericValue = HemisphereMultiplier.apply(numericValue,
        getHemisphere(value, this.hemisphere));
    }

    return numericValue;
  }
}
