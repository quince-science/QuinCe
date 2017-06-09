package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;

/**
 * Class to define a set of InstrumentFiles when creating an instrument
 * @author Steve Jones
 *
 */
public class InstrumentFileSet extends ArrayList<FileDefinitionBuilder> {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -2644317280574565531L;

	/**
	 * Simple constructor to create an empty set
	 */
	protected InstrumentFileSet() {
		super();
	}
	
	/**
	 * Store a file definition in the file set, replacing
	 * any existing versions of that definition
	 * @param file The file definition
	 */
	protected void storeFile(FileDefinitionBuilder file) {
		
		// Remove the existing file if it exists
		// Files are matched by their description only, so this
		// will find any file with the same description as the one passed in
		remove(file);
		
		// Add the passed in file
		add(file);
	}
	
	/**
	 * Determine whether or not this file set contains a file
	 * with the specified description
	 * @param description The description to find
	 * @return {@code true} if a file with the description exists; {@code false} otherwise
	 */
	public boolean containsFileDescription(String description) {
		boolean found = false;
		
		for (FileDefinitionBuilder file : this) {
			if (file.getFileDescription().equalsIgnoreCase(description)) {
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	/**
	 * Retrieve the file definition with the given description
	 * @param description The file description
	 * @return The file definition, or {@code null} if no matching description is found
	 */
	public FileDefinitionBuilder get(String description) {
		FileDefinitionBuilder result = null;
		
		for (FileDefinitionBuilder file : this) {
			if (file.getFileDescription().equalsIgnoreCase(description)) {
				result = file;
				break;
			}
		}
		
		return result;
	}
}
