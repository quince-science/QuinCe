package uk.ac.exeter.QuinCe.data.Files;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Exception raised for errors in extracting raw data files. Does not include
 * I/O errors.
 * @author Steve Jones
 *
 */
public class RawDataFileException extends Exception {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -4488468987912051593L;

	/**
	 * Basic constructor
	 * @param line The line number on which the error occurred
	 * @param message The error message
	 */
	public RawDataFileException(int line, String message) {
		super("Line " + line + ": " + message);
	}
	
	/**
	 * Constructor with an underlying cause
	 * @param line The line number on which the error occurred
	 * @param message The error message
	 * @param cause The underlying cause
	 */
	public RawDataFileException(int line, String message, Throwable cause) {
		super("Line " + line + ": " + message, cause);
	}

	/**
	 * Constructor for when a date search fails to find a matching line in the data file
	 * @param date The date/time that was being searched for
	 */
	public RawDataFileException(Calendar date) {
		super("Cannot find line with date: " + DateTimeUtils.formatDateTime(date));
	}
}
