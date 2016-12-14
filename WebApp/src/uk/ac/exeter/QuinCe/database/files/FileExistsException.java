package uk.ac.exeter.QuinCe.database.files;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;

public class FileExistsException extends Exception {

	private static final long serialVersionUID = -5136779592604842077L;

	/**
	 * The exception message
	 */
	private String message;
	
	public FileExistsException(DataSource dataSource, long instrumentID, String fileName) {
		super();
		try {
			String instrumentName = InstrumentDB.getInstrument(dataSource, instrumentID).getName();
			message = "File '" + fileName + "' already exists for instrument '" + instrumentName + "'";
		} catch (Exception e) {
			message = "File '" + fileName + "' already exists for this instrument";
		}
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	
}
