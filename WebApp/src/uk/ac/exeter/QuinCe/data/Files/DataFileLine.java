package uk.ac.exeter.QuinCe.data.Files;

import java.time.LocalDateTime;

/**
 * Class representing a specific line in a data file
 * @author Steve Jones
 *
 */
public class DataFileLine {

	/**
	 * The file that the line is from
	 */
	private DataFile file;
	
	/**
	 * The line number
	 */
	private int line;
	
	/**
	 * Basic constructor
	 * @param file The data file
	 * @param line The line number
	 */
	public DataFileLine(DataFile file, int line) {
		this.file = file;
		this.line = line;
	}
	
	/**
	 * Get the date of the line
	 * @return The date
	 * @throws DataFileException If the date cannot be extracted
	 */
	public LocalDateTime getDate() throws DataFileException {
		return file.getDate(line);
	}
}
