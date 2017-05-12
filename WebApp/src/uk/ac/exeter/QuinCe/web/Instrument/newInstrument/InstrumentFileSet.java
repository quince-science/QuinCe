package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.TreeSet;

/**
 * Class to define a set of InstrumentFiles when creating an instrument
 * @author Steve Jones
 *
 */
public class InstrumentFileSet extends TreeSet<FileDefinitionBuilder> {

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
		if (contains(file)) {
			remove(file);
		}
		
		// Add the passed in file
		add(file);
	}
}
