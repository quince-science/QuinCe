package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Represents a single sensor value
 * @author Steve Jones
 *
 */
public class SensorValue {

  /**
   * The ID of the dataset that the sensor value is in
   */
  private final long datasetId;

  /**
   * The ID of the column that the value is in. Either the ID
   * of a row in the {@code file_column} table, or a special value
   * (e.g. for lon/lat)
   */
  private final long columnId;

  /**
   * The time that the value was measured
   */
  private final LocalDateTime time;

  /**
   * The value (can be null)
   */
  private final String value;

  public SensorValue(long datasetId, long columnId, LocalDateTime time, String value) {
    this.datasetId = datasetId;
    this.columnId = columnId;
    this.time = time;
    this.value = value;
  }

  public long getDatasetId() {
    return datasetId;
  }

  public long getColumnId() {
    return columnId;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public String getValue() {
    return value;
  }

  /**
   * Get the plain VALUES SQL string for this object.
   * Assumes that the field order is the same as the constructor.
   * @return
   */
  public StringBuilder getSqlValues() {
    StringBuilder sqlValues = new StringBuilder();
    sqlValues.append('(');
    sqlValues.append(datasetId);
    sqlValues.append(',');
    sqlValues.append(columnId);
    sqlValues.append(',');
    sqlValues.append(DateTimeUtils.dateToLong(time));
    sqlValues.append(',');
    if (null == value) {
      sqlValues.append("NULL");
    } else {
      sqlValues.append('\'');
      sqlValues.append(value);
      sqlValues.append('\'');
    }
    sqlValues.append(')');

    return sqlValues;
  }
}
