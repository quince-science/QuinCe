package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.List;

import uk.ac.exeter.QuinCe.utils.MathUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Abstract class for position specifications. Longitudinal and latitudinal
 * positions share much similar functionality.
 *
 * @author Steve Jones
 *
 */
public abstract class PositionSpecification {

  /**
   * Unknown position format value
   */
  public static final int NO_FORMAT = -1;

  /**
   * The index of the column containing the position value
   */
  protected int valueColumn;

  /**
   * The index of the column containing the hemisphere
   */
  protected int hemisphereColumn;

  /**
   * The position format
   */
  protected int format;

  /**
   * Creates an empty position specification
   */
  protected PositionSpecification() {
    valueColumn = -1;
    hemisphereColumn = -1;
    format = NO_FORMAT;
  }

  /**
   * Constructor for a complete specification
   *
   * @param format           The format
   * @param valueColumn      The value column
   * @param hemisphereColumn The hemisphere column
   * @throws PositionException If the specification is incomplete or invalid
   */
  protected PositionSpecification(int format, int valueColumn, int hemisphereColumn) throws PositionException {
    setFormat(format);
    this.valueColumn = valueColumn;
    this.hemisphereColumn = hemisphereColumn;
    if (!specificationComplete()) {
      throw new PositionException("Specification is not complete");
    }
  }

  /**
   * Get the position format
   *
   * @return The position format
   */
  public int getFormat() {
    return format;
  }

  /**
   * Set the format for this position specification
   *
   * @param format The format code
   * @throws InvalidPositionFormatException If the format is invalid
   */
  public void setFormat(int format) throws InvalidPositionFormatException {
    if (!formatValid(format)) {
      throw new InvalidPositionFormatException(format);
    }

    this.format = format;
  }

  /**
   * Determine whether a given format identifier is valid
   *
   * @param format The format identifier
   * @return {@code true} if the format is valid; {@code false} if it is not
   */
  public abstract boolean formatValid(int format);

  /**
   * Determines whether or not a hemisphere column is required for this
   * specification's format
   *
   * @return {@code true} if a hemisphere column is required; {@code false} if it
   *         is not
   */
  public abstract boolean hemisphereRequired();

  /**
   * Determines whether or not this specification is complete, i.e. all required
   * column indices are supplied
   *
   * @return {@code true} if the specification is complete; {@code false} if it is
   *         not
   */
  public boolean specificationComplete() {
    boolean complete = true;

    if (valueColumn == -1) {
      complete = false;
    }

    if (hemisphereRequired() && hemisphereColumn == -1) {
      complete = false;
    }

    return complete;
  }

  /**
   * Get the column for the position's value
   *
   * @return The value column
   */
  public int getValueColumn() {
    return valueColumn;
  }

  /**
   * Set the column for the position's value
   *
   * @param valueColumn The value column
   */
  public void setValueColumn(int valueColumn) {
    this.valueColumn = valueColumn;
  }

  /**
   * Get the column for the position's hemisphere
   *
   * @return The hemisphere column
   */
  public int getHemisphereColumn() {
    return hemisphereColumn;
  }

  /**
   * Set the column for the position's hemisphere
   *
   * @param hemisphereColumn The hemisphere column
   */
  public void setHemisphereColumn(int hemisphereColumn) {
    this.hemisphereColumn = hemisphereColumn;
  }

  /**
   * Reset the value column
   */
  public void clearValueColumn() {
    valueColumn = -1;
    clearHemisphereColumn();
    format = NO_FORMAT;
  }

  /**
   * Reset the hemisphere column
   */
  public void clearHemisphereColumn() {
    hemisphereColumn = -1;
  }

  /**
   * Get the position value from a given line
   *
   * @param line The line
   * @return The position value
   * @throws PositionException If the position cannot be extracted, or is invalid
   */
  public String getValue(List<String> line) throws PositionException {

    String stringValue = line.get(getValueColumn()).trim();
    String hemisphereValue = null;
    if (getHemisphereColumn() > -1) {
      hemisphereValue = line.get(getHemisphereColumn()).trim();
    }

    PositionParser parser = getParser();
    double parsedValue = parser.parsePosition(stringValue, hemisphereValue);

    if (!MathUtils.checkRange(parsedValue, getMin(), getMax())) {
      throw new PositionParseException("Position out of range " + parsedValue);
    }

    // Handle the corner case where rounding ends up with a value of negative
    // zero
    String result = StringUtils.formatNumber(parsedValue);
    if (result.equals("-0.000")) {
      result = "0.000";
    } else if (result.equals("-180.000")) {
      result = "180.000";
    }

    return fixNegatives(result);
  }

  protected String fixNegatives(String value) {
    return value.equals("-0.000") ? "0.000" : value;
  }

  protected abstract PositionParser getParser() throws PositionException;

  protected abstract double getMin();

  protected abstract double getMax();
}
