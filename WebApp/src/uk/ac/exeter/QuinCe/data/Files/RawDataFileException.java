package uk.ac.exeter.QuinCe.data.Files;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

@Deprecated
public class RawDataFileException extends Exception {
	
	private static final long serialVersionUID = -4488468987912051593L;

	public RawDataFileException(int line, String message) {
		super("Line " + line + ": " + message);
	}
	
	public RawDataFileException(int line, String message, Throwable cause) {
		super("Line " + line + ": " + message, cause);
	}

	@Deprecated
	public RawDataFileException(Calendar date) {
		super("Cannot find line with date: " + DateTimeUtils.formatDateTime(date));
	}
}
