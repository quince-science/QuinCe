package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public class DecimalDegreesParser extends PositionParser {

  private boolean negativeAllowed = false;

  protected DecimalDegreesParser(boolean negativeAllowed) {
    super();
    this.negativeAllowed = negativeAllowed;
  }

  protected DecimalDegreesParser(HemisphereMultiplier hemisphereMultiplierGenerator) {
    super(hemisphereMultiplierGenerator);
    negativeAllowed = false;
  }

  @Override
  public double getNumericValue(String value) throws PositionParseException {

    double result;

    try {
      result = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new PositionParseException(value);
    }

    if (!negativeAllowed && result < 0) {
      throw new PositionParseException("Invalid position " + result);
    }

    return result;
  }
}
