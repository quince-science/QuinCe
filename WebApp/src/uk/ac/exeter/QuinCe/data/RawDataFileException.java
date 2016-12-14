package uk.ac.exeter.QuinCe.data;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class RawDataFileException extends Exception {
	
	private static final long serialVersionUID = -4488468987912051593L;

	public RawDataFileException(int line, String message) {
		super("Line " + line + ": " + message);
	}
	
	public RawDataFileException(int line, String message, Throwable cause) {
		super("Line " + line + ": " + message, cause);
	}

	public RawDataFileException(Calendar date) {
		super("Cannot find line with date: " + DateTimeUtils.formatDateTime(date));
	}
}
