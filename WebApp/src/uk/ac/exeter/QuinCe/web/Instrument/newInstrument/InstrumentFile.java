package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

/**
 * Holds a description of a sample data file uploaded during the
 * creation of a new instrument
 * @author Steve Jones
 *
 */
public class InstrumentFile implements Comparable<InstrumentFile> {

	/**
	 * The name used to identify files of this type
	 */
	private String fileDescription;

	/**
	 * Comparison is based on the file description
	 */
	@Override
	public int compareTo(InstrumentFile o) {
		return fileDescription.compareTo(o.fileDescription);
	}
	
}
