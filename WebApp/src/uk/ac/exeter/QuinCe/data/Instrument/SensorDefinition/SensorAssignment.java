package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

/**
 * Records the data file and column number that have been assigned a particular sensor role
 * @author Steve Jones
 *
 */
public class SensorAssignment {

	/**
	 * The data file
	 */
	private String dataFile;

	/**
	 * The column number (zero-based)
	 */
	private int column;

	/**
	 * The column in which the data will be stored in the database
	 */
	private int databaseColumn = -1;

	/**
	 * The name of the sensor
	 */
	private String sensorName;

	/**
	 * Indicates whether or not values from this
	 * sensor require post-calibration adjustments applied to them
	 */
	private boolean postCalibrated;

	/**
	 * The answer to the Depends Question
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
	 * @param dataFile The data file
	 * @param column The column number
	 * @param sensorName The name of the sensor
	 * @param postCalibrated Specifies whether or not values should be calibrated by QuinCe
	 * @param primary Specifies whether this is a primary or fallback sensor
	 * @param dependsQuestionAnswer The answer to the Depends Question
	 * @param missingValue The missing value String
	 */
	public SensorAssignment(String dataFile, int column, String sensorName, boolean postCalibrated, boolean primary, boolean dependsQuestionAnswer, String missingValue) {
		this.dataFile = dataFile;
		this.column = column;
		this.sensorName = sensorName;
		this.postCalibrated = postCalibrated;
		this.primary = primary;
		this.dependsQuestionAnswer = dependsQuestionAnswer;
		this.missingValue = missingValue;
	}

	/**
	 * Simple constructor
	 * @param dataFile The data file
	 * @param fileColumn The column number in the file
	 * @param databaseColumn The column where the sensor's data will be stored in the database
	 * @param sensorName The name of the sensor
	 * @param postCalibrated Specifies whether or not values should be calibrated by QuinCe
	 * @param primary Specifies whether this is a primary or fallback sensor
	 * @param dependsQuestionAnswer The answer to the Depends Question
	 * @param missingValue The missing value String
	 */
	public SensorAssignment(String dataFile, int fileColumn, int databaseColumn, String sensorName, boolean postCalibrated, boolean primary, boolean dependsQuestionAnswer, String missingValue) {
		this.dataFile = dataFile;
		this.column = fileColumn;
		this.databaseColumn = databaseColumn;
		this.sensorName = sensorName;
		this.postCalibrated = postCalibrated;
		this.primary = primary;
		this.dependsQuestionAnswer = dependsQuestionAnswer;
		this.missingValue = missingValue;
	}

	/**
	 * Get the data file
	 * @return The data file
	 */
	public String getDataFile() {
		return dataFile;
	}

	/**
	 * Get the column number
	 * @return The column number
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Get the name of the sensor
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
	 * Determine whether or not values from this
	 * sensor require post-calibration adjustments applied to them
	 * @return {@code true} if calibration adjustments are required; {@code false} if they are not
	 */
	public boolean getPostCalibrated() {
		return postCalibrated;
	}

	/**
	 * Set the answer to the Depends Question
	 * @param dependsQuestionAnswer The answer
	 * @see SensorType#getDependsQuestion()
	 */
	public void setDependsQuestionAnswer(boolean dependsQuestionAnswer) {
		this.dependsQuestionAnswer = dependsQuestionAnswer;
	}

	/**
	 * Get the answer to the Depends Question
	 * @return The answer
	 * @see SensorType#getDependsQuestion()
	 */
	public boolean getDependsQuestionAnswer() {
		return dependsQuestionAnswer;
	}

	/**
	 * Determines whether or not this is a primary sensor
	 * @return {@code true} if this is a primary sensor; {@code false} if it is a fallback sensor
	 */
	public boolean isPrimary() {
		return primary;
	}

	/**
	 * Get the column index where this sensor's data will be stored in the database
	 * @return The database column index
	 */
	public int getDatabaseColumn() {
		return databaseColumn;
	}

	/**
	 * Set the column index where this sensor's data will be stored in the database
	 * @param databaseColumn The database column index
	 */
	public void setDatabaseColumn(int databaseColumn) {
		this.databaseColumn = databaseColumn;
	}

	/**
	 * Get the missing value String
	 * @return The missing value String
	 */
	public String getMissingValue() {
		return missingValue;
	}

	/**
	 * Set the missing value String
	 * @param missingValue The missing value String
	 */
	public void setMissingValue(String missingValue) {
		this.missingValue = missingValue;
	}
}
