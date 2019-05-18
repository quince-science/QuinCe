package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Class to represent a file column.
 * Contains the file column and sensor type references
 * @author Steve Jones
 *
 */
public class FileColumn {

  /**
   * The database ID of the column
   */
  private final long columnId;

  /**
   * The user-entered name for the column
   */
  private final String columnName;

  /**
   * The database ID of the Sensor Type that this column contains
   */
  private final long sensorTypeId;

  /**
   * The name of the sensor type that this column contains
   */
  private final String sensorTypeName;

  public FileColumn(long columnId, String columnName,
    long sensorTypeId, String sensorTypeName) {

    this.columnId = columnId;
    this.columnName = columnName;
    this.sensorTypeId = sensorTypeId;
    this.sensorTypeName = sensorTypeName;
  }

  /**
   * Get the column database ID
   * @return The column ID
   */
  public long getColumnId() {
    return columnId;
  }

  /**
   * Get the column name
   * @return The column name
   */
  public String getColumnName() {
    return columnName;
  }

  /**
   * Get the sensor type database ID
   * @return The sensor type ID
   */
  public long getSensorTypeId() {
    return sensorTypeId;
  }

  /**
   * Get the sensor type name
   * @return The sensor type name
   */
  public String getSensorTypeName() {
    return sensorTypeName;
  }

  @Override
  public String toString() {
    return columnName;
  }
}
