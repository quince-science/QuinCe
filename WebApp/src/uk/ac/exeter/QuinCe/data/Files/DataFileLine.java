package uk.ac.exeter.QuinCe.data.Files;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;

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
	
	/**
	 * Determines whether or not this line contains a measurement
	 * @return {@code true} if the line contains a measurement; {@code false} otherwise
	 * @throws DataFileException If the data cannot be extracted from the file
	 * @throws FileDefinitionException If the run type is invalid
	 */
	public boolean isMeasurement() throws DataFileException, FileDefinitionException {
		
		boolean measurement = true;
		
		FileDefinition fileDefinition = file.getFileDefinition();
		
		// If a file does not have run types, it can always be used
		// for a measurement. So we only check files that have them.
		if (fileDefinition.hasRunTypes()) {
			RunTypeCategory runType = file.getRunType(line);
			measurement = runType.isMeasurementType();
		}
			
		return measurement;
	}
}
