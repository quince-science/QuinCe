package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.TreeSet;

/**
 * Class to define a set of InstrumentFiles when creating an instrument
 * @author Steve Jones
 *
 */
public class InstrumentFileSet extends TreeSet<InstrumentFile> {

	/**
	 * Simple constructor to create an empty set
	 */
	protected InstrumentFileSet() {
		super();
	}
	
	/**
	 * Determine whether any of the files in the set have not yet been fully defined
	 * @return {@code true} if any of the files are not fully defined; {@code false} if all files are fully defined
	 */
	protected boolean anyFilesUndefined() {
		boolean result = false;
		
		if (size() == 0) {
			result = true;
		} else {
			for (InstrumentFile file : this) {
				if (!file.fileDefined()) {
					result = true;
					break;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieve the first file in the set that has not been fully defined.
	 * If there are no files in the set, or all the files have been defined, the method returns {@code null}
	 * @return The first undefined file in the set
	 */
	protected InstrumentFile getFirstUndefinedFile() {
		InstrumentFile result = null;
		
		for (InstrumentFile file : this) {
			if (!file.fileDefined()) {
				result = file;
				break;
			}
		}
		
		return result;
	}
}
