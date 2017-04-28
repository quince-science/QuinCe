package uk.ac.exeter.QuinCe.data.Instrument;

/**
 * Holds a description of a sample data file uploaded during the
 * creation of a new instrument
 * @author Steve Jones
 *
 */
public class InstrumentFile implements Comparable<InstrumentFile> {

	/**
	 * The default description for new files
	 */
	private static final String DEFAULT_DESCRIPTION = "Data File";
	
	/**
	 * A unique ID used to identify this file in the parent file set
	 */
	private long uniqueId;
	
	/**
	 * The name used to identify files of this type
	 */
	private String fileDescription;

	/**
	 * Create a new file with the default description
	 */
	public InstrumentFile() {
		this.uniqueId = System.currentTimeMillis();
		this.fileDescription = DEFAULT_DESCRIPTION;
	}
	
	/**
	 * Create a new file with the given description
	 * @param fileDescription The file description
	 */
	public InstrumentFile(String fileDescription) {
		this.uniqueId = System.currentTimeMillis();
		this.fileDescription = fileDescription;
	}
	
	/**
	 * Get the description for this file
	 * @return The file description
	 */
	public String getFileDescription() {
		return fileDescription;
	}
	
	/**
	 * Set the description for this file
	 * @param fileDescription The file description
	 */
	public void setFileDescription(String fileDescription) {
		this.fileDescription = fileDescription;
	}
	
	/**
	 * Comparison is based on the file description. The comparison is case insensitive.
	 */
	@Override
	public int compareTo(InstrumentFile o) {
		return fileDescription.toLowerCase().compareTo(o.fileDescription.toLowerCase());
	}
	
	/**
	 * Equals compares on the unique ID
	 */
	@Override
	public boolean equals(Object o) {
		boolean result = true;
		
		if (null == o) {
			result = false;
		} else if (!(o instanceof InstrumentFile)) {
			result = false;
		} else {
			InstrumentFile oFile = (InstrumentFile) o;
			result = oFile.uniqueId == uniqueId;
		}
		
		return result;
	}
	
	/**
	 * Determines whether or not this instrument file has been fully defined.
	 * This includes the existence of file data, specification of headers etc.
	 * @return {@code true} if this file has been fully defined; {@code false} otherwise.
	 */
	public boolean fileDefined() {
		return false;
	}
}
