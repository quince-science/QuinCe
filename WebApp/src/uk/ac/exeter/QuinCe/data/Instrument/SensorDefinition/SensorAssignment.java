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
	 * Simple constructor
	 * @param dataFile The data file
	 * @param column The column number
	 */
	public SensorAssignment(String dataFile, int column) {
		this.dataFile = dataFile;
		this.column = column;
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
}
