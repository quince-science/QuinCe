package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Objects;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;

/**
 * Contains all details for a column header.
 *
 * <p>
 * This class is used for both the web application (e.g. QC pages) and exports.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class ColumnHeading {

  /**
   * The column's unique identifer.
   *
   * <p>
   * For sensors or diagnostics, this is the column's database ID. For
   * calculation parameters, it is the ID generated from the parameter's
   * {@link DataReducer}.
   * </p>
   */
  private final long id;

  /**
   * The column's short name
   */
  private final String shortName;

  /**
   * The column's human readable name
   */
  private final String longName;

  /**
   * The column's code (usually from a standard vocabulary).
   */
  private final String codeName;

  public final boolean hasQC;

  /**
   * The units for the column.
   */
  private final String units;

  public ColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean hasQC) {

    this.id = id;
    this.shortName = shortName;
    this.longName = longName;
    this.codeName = codeName;
    this.units = units;
    this.hasQC = hasQC;
  }

  /**
   * Copy constructor
   *
   * @param heading
   *          The source heading
   */
  public ColumnHeading(ColumnHeading heading) {
    this.id = heading.id;
    this.shortName = heading.shortName;
    this.longName = heading.longName;
    this.codeName = heading.codeName;
    this.units = heading.units;
    this.hasQC = heading.hasQC;
  }

  /**
   * Get the column ID
   *
   * @return The column ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the column's long name
   *
   * @return The long name
   */
  public String getLongName() {
    return longName;
  }

  /**
   * Get the column's short name
   *
   * @return The short name
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Get the column's code name
   *
   * @return The code name
   */
  public String getCodeName() {
    return codeName;
  }

  /**
   * Get the units for the column
   *
   * @return The column's units
   */
  public String getUnits() {
    return units;
  }

  /**
   * Get the column's short name, optionally adding the units
   *
   * @param includeUnits
   *          Indicates whether units should be included
   * @return The short name
   */
  public String getShortName(boolean includeUnits) {
    return includeUnits ? buildUnitsString(shortName) : shortName;
  }

  /**
   * Get the column's long name, optionally adding the units
   *
   * @param includeUnits
   *          Indicates whether units should be included
   * @return The long name
   */
  public String getLongName(boolean includeUnits) {
    return includeUnits ? buildUnitsString(longName) : longName;
  }

  /**
   * Get the column's code name, optionally adding the units
   *
   * @param includeUnits
   *          Indicates whether units should be included
   * @return The code name
   */
  public String getCodeName(boolean includeUnits) {
    return includeUnits ? buildUnitsString(codeName) : codeName;
  }

  public boolean hasQC() {
    return hasQC;
  }

  private String buildUnitsString(String base) {
    StringBuilder result = new StringBuilder(base);

    if (null != units && units.length() > 0) {
      result.append(" [");
      result.append(units);
      result.append(']');
    }

    return result.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof ColumnHeading))
      return false;
    ColumnHeading other = (ColumnHeading) obj;
    return id == other.id;
  }

  @Override
  public String toString() {
    return getShortName();
  }
}
