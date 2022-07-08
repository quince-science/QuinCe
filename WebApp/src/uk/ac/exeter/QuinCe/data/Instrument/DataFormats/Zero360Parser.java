package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public class Zero360Parser extends DecimalDegreesParser {

  protected Zero360Parser() {
    super(false);
  }

  @Override
  public double getNumericValue(String value) throws PositionParseException {

    double parsed = super.getNumericValue(value);

    if (parsed >= 360D) {
      throw new PositionParseException(value);
    }

    if (parsed > 180) {
      parsed = (360 - parsed) * -1;
    }

    return parsed;
  }

}
