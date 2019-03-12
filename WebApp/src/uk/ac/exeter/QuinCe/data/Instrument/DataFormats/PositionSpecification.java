package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Abstract class for position specifications. Longitudinal and latitudinal
 * positions share much similar functionality.
 *
 * @author Steve Jones
 *
 */
public abstract class PositionSpecification {

  /**
   * Indicates the longitude for setting the hemisphere column
   */
  public static final int COORD_LONGITUDE = 0;

  /**
   * Indicates the latitude for setting the hemisphere column
   */
  public static final int COORD_LATITUDE = 1;

  /**
   * Unknown position format value
   */
  public static final int FORMAT_UNKNOWN = -1;

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
    format = FORMAT_UNKNOWN;
  }

  /**
   * Constructor for a complete specification
   * @param format The format
   * @param valueColumn The value column
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
   * @return The position format
   */
  public int getFormat() {
    return format;
  }

  /**
   * Set the format for this position specification
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
   * @param format The format identifier
   * @return {@code true} if the format is valid; {@code false} if it is not
   */
  public abstract boolean formatValid(int format);

  /**
   * Determines whether or not a hemisphere column is required for
   * this specification's format
   * @return {@code true} if a hemisphere column is required; {@code false} if it is not
   */
  public abstract boolean hemisphereRequired();

  /**
   * Determines whether or not this specification is complete,
   * i.e. all required column indices are supplied
   * @return {@code true} if the specification is complete; {@code false} if it is not
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
   * @return The value column
   */
  public int getValueColumn() {
    return valueColumn;
  }

  /**
   * Set the column for the position's value
   * @param valueColumn The value column
   */
  public void setValueColumn(int valueColumn) {
    this.valueColumn = valueColumn;
  }

  /**
   * Get the column for the position's hemisphere
   * @return The hemisphere column
   */
  public int getHemisphereColumn() {
    return hemisphereColumn;
  }

  /**
   * Set the column for the position's hemisphere
   * @param hemisphereColumn The hemisphere column
   */
  public void setHemisphereColumn(int hemisphereColumn) {
    this.hemisphereColumn = hemisphereColumn;
  }

  /**
   * Get this representation as a GSON JsonObject
   *
   * <p>
   * The JSON format is as follows:
   * </p>
   *
   * <pre>
   *   {
   *     "format": <position format>,
   *     "valueColumn": <value column index>,
   *     "hemisphereColumn": <hemisphere column index>
   *   }
   * </pre>
   * <p>
   * The format will be the integer value corresponding to the chosen format.
   * The JSON processor will need to know how to translate these.
   * </p>
   *
   * @return The JSON string
   */
  public JsonObject getJsonObject() {

    JsonObject json = new JsonObject();
    json.addProperty("format", format);
    json.addProperty("valueColumn", valueColumn);
    json.addProperty("hemisphereRequired", hemisphereRequired());
    json.addProperty("hemisphereColumn", hemisphereColumn);
    return json;
  }

  /**
   * Reset the value column
   */
  public void clearValueColumn() {
    valueColumn = -1;
    clearHemisphereColumn();
    format = FORMAT_UNKNOWN;
  }

  /**
   * Reset the hemisphere column
   */
  public void clearHemisphereColumn() {
    hemisphereColumn = -1;
  }

  /**
   *  Get the position value from a given line
   * @param line The line
   * @return The position value
   * @throws PositionException If the position cannot be extracted, or is invalid
   */
  public abstract double getValue(List<String> line) throws PositionException;
}
