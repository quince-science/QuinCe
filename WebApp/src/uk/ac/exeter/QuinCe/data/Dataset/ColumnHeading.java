package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Collection;

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
   * The column's unique identifier.
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

  private final boolean hasQC;

  private final boolean includeType;

  /**
   * The units for the column.
   */
  private final String units;

  public ColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean hasQC, boolean includeType) {

    this.id = id;
    this.shortName = shortName;
    this.longName = longName;
    this.codeName = codeName;
    this.units = null == units ? null : units.trim();
    this.hasQC = hasQC;
    this.includeType = includeType;
  }

  /**
   * Copy constructor
   *
   * @param heading
   *          The source heading
   */
  public ColumnHeading(ColumnHeading heading) {
    this.id = heading.getId();
    this.shortName = heading.getShortName();
    this.longName = heading.getLongName();
    this.codeName = heading.getCodeName();
    this.units = heading.getUnits();
    this.hasQC = heading.hasQC();
    this.includeType = heading.includeType();
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

  public boolean includeType() {
    return includeType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((codeName == null) ? 0 : codeName.hashCode());
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ColumnHeading other = (ColumnHeading) obj;
    if (codeName == null) {
      if (other.codeName != null)
        return false;
    } else if (!codeName.equals(other.codeName))
      return false;
    if (id != other.id)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return getShortName();
  }

  public static boolean containsColumnWithCode(
    Collection<? extends ColumnHeading> columns, String code) {

    boolean result = false;

    for (ColumnHeading column : columns) {
      if (column.getCodeName().equals(code)) {
        result = true;
      }
    }

    return result;
  }
}
