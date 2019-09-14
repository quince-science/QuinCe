package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Records the data file and column number that have been assigned a particular
 * sensor role
 * 
 * @author Steve Jones
 *
 */
public class SensorAssignment {

  /**
   * The database ID of this sensor assignment
   */
  private long databaseId = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The data file
   */
  private String dataFile;

  /**
   * The column number (zero-based)
   */
  private int column;

  /**
   * The name of the sensor
   */
  private String sensorName;

  /**
   * The answer to the Depends Question
   * 
   * @see SensorType#getDependsQuestion
   */
  private boolean dependsQuestionAnswer = false;

  /**
   * Indicates whether this is a primary or fallback sensor
   */
  private boolean primary = true;

  /**
   * The String that indicates a missing value
   */
  private String missingValue = null;

  /**
   * Simple constructor
   * 
   * @param dataFile
   *          The data file
   * @param column
   *          The column number
   * @param sensorName
   *          The name of the sensor
   * @param postCalibrated
   *          Specifies whether or not values should be calibrated by QuinCe
   * @param primary
   *          Specifies whether this is a primary or fallback sensor
   * @param dependsQuestionAnswer
   *          The answer to the Depends Question
   * @param missingValue
   *          The missing value String
   */
  public SensorAssignment(String dataFile, int column, String sensorName,
    boolean primary, boolean dependsQuestionAnswer, String missingValue) {

    this.dataFile = dataFile;
    this.column = column;
    this.sensorName = sensorName;
    this.primary = primary;
    this.dependsQuestionAnswer = dependsQuestionAnswer;
    setMissingValue(missingValue);
  }

  /**
   * Simple constructor
   * 
   * @param databaseId
   *          The assignment's datbaase ID
   * @param dataFile
   *          The data file
   * @param fileColumn
   *          The column number in the file
   * @param databaseColumn
   *          The column where the sensor's data will be stored in the database
   * @param sensorName
   *          The name of the sensor
   * @param postCalibrated
   *          Specifies whether or not values should be calibrated by QuinCe
   * @param primary
   *          Specifies whether this is a primary or fallback sensor
   * @param dependsQuestionAnswer
   *          The answer to the Depends Question
   * @param missingValue
   *          The missing value String
   */
  public SensorAssignment(long databaseId, String dataFile, int fileColumn,
    String sensorName, boolean primary, boolean dependsQuestionAnswer,
    String missingValue) {

    this.databaseId = databaseId;
    this.dataFile = dataFile;
    this.column = fileColumn;
    this.sensorName = sensorName;
    this.primary = primary;
    this.dependsQuestionAnswer = dependsQuestionAnswer;
    setMissingValue(missingValue);
  }

  /**
   * Get the database ID of this sensor assignment
   * 
   * @return The assignment's database ID
   */
  public long getDatabaseId() {
    return databaseId;
  }

  /**
   * Set the database ID of this assignment
   * 
   * @param databaseId
   *          The database ID
   */
  public void setDatabaseId(long databaseId) {
    this.databaseId = databaseId;
  }

  /**
   * Get the data file
   * 
   * @return The data file
   */
  public String getDataFile() {
    return dataFile;
  }

  /**
   * Get the column number
   * 
   * @return The column number
   */
  public int getColumn() {
    return column;
  }

  /**
   * Get the name of the sensor
   * 
   * @return The sensor name
   */
  public String getSensorName() {
    String result = "";

    if (null != sensorName) {
      result = sensorName;
    }

    return result;
  }

  /**
   * Set the answer to the Depends Question
   * 
   * @param dependsQuestionAnswer
   *          The answer
   * @see SensorType#getDependsQuestion()
   */
  public void setDependsQuestionAnswer(boolean dependsQuestionAnswer) {
    this.dependsQuestionAnswer = dependsQuestionAnswer;
  }

  /**
   * Get the answer to the Depends Question
   * 
   * @return The answer
   * @see SensorType#getDependsQuestion()
   */
  public boolean getDependsQuestionAnswer() {
    return dependsQuestionAnswer;
  }

  /**
   * Determines whether or not this is a primary sensor
   * 
   * @return {@code true} if this is a primary sensor; {@code false} if it is a
   *         fallback sensor
   */
  public boolean isPrimary() {
    return primary;
  }

  /**
   * Get the missing value String
   * 
   * @return The missing value String
   */
  public String getMissingValue() {
    return missingValue;
  }

  /**
   * Set the missing value String
   * 
   * @param missingValue
   *          The missing value String
   */
  public void setMissingValue(String missingValue) {
    if (null == missingValue) {
      this.missingValue = "";
    } else {
      this.missingValue = missingValue;
    }
  }

  /**
   * Get the human-readable string describing an assignment to a data file and
   * named sensor
   *
   * @param dataFile
   *          The data file name
   * @param sensorName
   *          The sensor name
   * @return The description
   */
  public static String getTarget(String dataFile, String sensorName) {
    return dataFile + ": " + sensorName;
  }

  /**
   * Get the human-readable string describing this assignment
   * 
   * @return The assignment description
   */
  public String getTarget() {
    return getTarget(getDataFile(), getSensorName());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + column;
    result = prime * result + ((dataFile == null) ? 0 : dataFile.hashCode());
    result = prime * result + (int) (databaseId ^ (databaseId >>> 32));
    result = prime * result + (dependsQuestionAnswer ? 1231 : 1237);
    result = prime * result
      + ((missingValue == null) ? 0 : missingValue.hashCode());
    result = prime * result + (primary ? 1231 : 1237);
    result = prime * result
      + ((sensorName == null) ? 0 : sensorName.hashCode());
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
    SensorAssignment other = (SensorAssignment) obj;
    if (column != other.column)
      return false;
    if (dataFile == null) {
      if (other.dataFile != null)
        return false;
    } else if (!dataFile.equals(other.dataFile))
      return false;
    if (databaseId != other.databaseId)
      return false;
    if (dependsQuestionAnswer != other.dependsQuestionAnswer)
      return false;
    if (missingValue == null) {
      if (other.missingValue != null)
        return false;
    } else if (!missingValue.equals(other.missingValue))
      return false;
    if (primary != other.primary)
      return false;
    if (sensorName == null) {
      if (other.sensorName != null)
        return false;
    } else if (!sensorName.equals(other.sensorName))
      return false;
    return true;
  }
}
