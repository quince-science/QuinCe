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
	 * Simple constructor
	 * @param dataFile The data file
	 * @param column The column number
	 * @param postCalibrated Specifies whether or not values should be calibrated by QuinCe
	 */
	public SensorAssignment(String dataFile, int column, boolean postCalibrated, boolean primary) {
		this.dataFile = dataFile;
		this.column = column;
		this.postCalibrated = postCalibrated;
		this.primary = primary;
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
}
