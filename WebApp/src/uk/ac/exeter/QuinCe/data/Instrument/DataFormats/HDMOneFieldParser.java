package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public class HDMOneFieldParser extends HDMParser {

  private String hemisphere;

  protected HDMOneFieldParser(HemisphereMultiplier hemisphereMultiplier) {
    super(hemisphereMultiplier);
  }

  @Override
  protected String getHemisphere(String value, String dummy) throws PositionParseException {
    if (!parsed) {
      parse(value);
    }

    return hemisphere;
  }

  @Override
  protected void parseAction(String value) throws PositionParseException {

    // Split on whitespace
    String[] split = value.split("\\s+");

    if (split.length != 3) {
      throw new PositionParseException(value);
    }

    hemisphere = split[0];
    degrees = Integer.parseInt(split[1]);
    minutes = Double.parseDouble(split[2]);
  }
}
