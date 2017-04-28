package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import org.primefaces.model.UploadedFile;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFile;

/**
 * This is a specialised instance of the InstrumentFile class
 * that provides special functions used only when an instrument
 * is being defined.
 * 
 * @author Steve Jones
 *
 */
public class InstrumentFileBuilder extends InstrumentFile {

	/**
	 * The default description for new files
	 */
	private static final String DEFAULT_DESCRIPTION = "Data File";
	
	/**
	 * The contents of the uploaded data file
	 */
	private String fileData = null;
	
	/**
	 * Create a new file with the default description
	 */
	public InstrumentFileBuilder() {
		super(DEFAULT_DESCRIPTION);
	}
	
	/**
	 * Determines whether or not this instrument file has been fully defined.
	 * This includes the existence of file data, specification of headers etc.
	 * @return {@code true} if this file has been fully defined; {@code false} otherwise.
	 */
	public boolean fileDefined() {
		return false;
	}
	
	/**
	 * Determines whether or not file data has been uploaded for this instrument file
	 * @return {@code true} if file data has been uploaded; {@code false} if it has not.
	 */
	public boolean getHasFileData() {
		return (null != fileData);
	}
	
	/**
	 * Store the uploaded data for this file
	 * @param fileData The file data
	 */
	public void setFileData(String fileData) {
		this.fileData = fileData;
	}
}
