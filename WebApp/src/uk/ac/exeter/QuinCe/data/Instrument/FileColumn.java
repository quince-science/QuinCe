package uk.ac.exeter.QuinCe.data.Instrument;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Class to represent a file column. Contains the file column and sensor type
 * references
 * 
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
   * The Sensor Type that this column contains
   */
  private final SensorType sensorType;

  public FileColumn(long columnId, String columnName, SensorType sensorType) {

    this.columnId = columnId;
    this.columnName = columnName;
    this.sensorType = sensorType;
  }

  /**
   * Get the column database ID
   * 
   * @return The column ID
   */
  public long getColumnId() {
    return columnId;
  }

  /**
   * Get the column name
   * 
   * @return The column name
   */
  public String getColumnName() {
    return columnName;
  }

  /**
   * Get the sensor type database ID
   * 
   * @return The sensor type ID
   */
  public SensorType getSensorType() {
    return sensorType;
  }

  @Override
  public String toString() {
    return columnName;
  }
}
